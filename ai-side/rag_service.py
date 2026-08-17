"""
=============================================================================
 rag_service.py — RAG 检索增强生成核心服务(改造自桌面 RAG 项目 rag.py)
=============================================================================
作用:构建完整的 RAG(Retrieval-Augmented Generation)执行链,是 AI 服务的核心。
     它将"检索 → 格式化 → 组装提示词 → 调用LLM → 解析输出"这条流水线串联起来,
     并支持多轮对话历史管理。

RAG 的核心思想:
  1. 用户提问 "如何辨别青花瓷真伪"
  2. 从向量数据库中检索相关文档(如 knowledge/青花瓷真伪鉴别.txt 中的段落)
  3. 将检索到的文档作为"参考资料"注入提示词
  4. LLM 基于参考资料 + 对话历史 生成高质量、有依据的回答
  5. 保存这次的问答到历史记录,供下一轮对话使用

架构概览:
  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌─────────────┐
  │  用户输入     │ →  │  向量检索     │ →  │  提示词模板   │ →  │  通义千问    │
  │  {input: }   │    │  retriever   │    │  + 历史记录   │    │  LLM 生成   │
  └──────────────┘    └──────────────┘    └──────────────┘    └─────────────┘

与桌面 RAG 项目的差异:
  1. session_id 由固定值改为动态 userId(str(userId)),每个用户独立历史
  2. 配置全部来自本服务的 config_data.py(知识库路径、模型名称等)
  3. 提供 ask(user_id, question) 方法,供 main.py 的 /chat 接口调用
  4. 移除了调试打印步骤(print_prompt),避免服务端刷屏

【跨文件关联】
  - 调用 vector_stores.py → VectorStoreService.get_retriever() 获取检索器
  - 调用 file_history_store.py → get_history() 获取聊天历史工厂函数
  - 调用 config_data.py → 模型名称、检索参数等配置
=============================================================================
"""

# ===== LangChain 核心组件导入 =====
# StrOutputParser: 将 LLM 返回的 AIMessage 对象解析为纯字符串
from langchain_core.output_parsers import StrOutputParser

# RunnablePassthrough: 透传数据(RunnableWithMessageHistory 传入的原始字典)
# RunnableWithMessageHistory: 包装器,自动管理对话历史的加载和保存
# RunnableLambda: 将普通 Python 函数包装成 LangChain 可串联的 Runnable 对象
from langchain_core.runnables import RunnablePassthrough, RunnableWithMessageHistory, RunnableLambda

# ===== 项目内部模块导入 =====
# get_history: 聊天历史工厂函数 → 来自 file_history_store.py(本地文件存储)
from file_history_store import get_history

# VectorStoreService: 向量检索服务 → 来自 vector_stores.py
# 【关联】封装了 ChromaDB 的"读"操作(检索),与 knowledge_base.py 的"写"操作(入库)对应
from vector_stores import VectorStoreService

# DashScopeEmbeddings: 阿里云文本嵌入模型客户端
# 【关联】必须与 knowledge_base.py 入库时使用完全相同的模型,否则向量空间不一致
from langchain_community.embeddings import DashScopeEmbeddings

# ===== 配置导入 =====
import config_data as config

# ChatPromptTemplate: 构建聊天提示词模板
# MessagesPlaceholder: 在模板中为历史消息列表预留位置
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

# ChatTongyi: LangChain 封装的阿里云通义千问聊天模型接口
from langchain_community.chat_models.tongyi import ChatTongyi


class RagService(object):
    """
    RAG 检索增强生成服务类

    这是 AI 服务的"大脑",负责:
      1. 初始化向量检索器、提示词模板、大语言模型
      2. 构建完整的 LCEL(LangChain Expression Language)执行链
      3. 包装多轮对话历史管理

    使用方式:
      service = RagService()
      answer = service.ask(1, "如何辨别青花瓷真伪")
    """

    def __init__(self):
        """
        初始化 RAG 服务

        完成四件事:
          1. 初始化向量检索服务 → 从知识库中检索相关文档
          2. 定义提示词模板 → 告诉LLM如何利用参考资料回答
          3. 初始化大语言模型 → 阿里云通义千问 qwen3-max
          4. 构建执行链 → 将检索、模板、模型串联起来
        """
        # ================================================================
        # 1. 初始化向量检索服务
        # ================================================================
        # 使用阿里云 DashScope text-embedding-v4 作为嵌入模型
        # 【关联】embedding 模型名称来自 config_data.py 的 config.embedding_model_name
        # 【关联】dashscope_api_key 来自 config_data.py(联调例外配置,为空时自动读环境变量)
        self.vector_service = VectorStoreService(
            embedding=DashScopeEmbeddings(
                model=config.embedding_model_name,
                dashscope_api_key=config.dashscope_api_key
            )
        )

        # ================================================================
        # 2. 定义提示词模板
        # ================================================================
        # ChatPromptTemplate.from_messages() 接收一个消息列表,
        # 每项是一个 (角色, 内容) 的元组或特殊占位符,按顺序排列
        self.prompt_template = ChatPromptTemplate.from_messages(
            [
                # ----- 第一条 system 消息 -----
                # 告诉 LLM 以参考资料为主进行回答
                # {context} 是占位符,运行时会被替换为检索到的文档内容(字符串)
                ("system", "以我提供的已知参考资料为主,简洁和专业的回答用户问题,参考资料:{context}"),

                # ----- 第二条 system 消息 -----
                # 提示 LLM 注意查看对话历史记录
                ("system", "并且我提供用户的对话历史记录,如下:"),

                # ----- MessagesPlaceholder:历史消息的占位符 -----
                # 运行时会被替换为之前的 HumanMessage 和 AIMessage 对象列表
                # 【关联】"history" 这个名称必须与 RunnableWithMessageHistory 的
                #        history_messages_key="history" 参数一致!
                MessagesPlaceholder("history"),

                # ----- 用户消息 -----
                # {input} 占位符,运行时被替换为当前用户输入的文本
                ("user", "请回答用户提问: {input}")
            ]
        )

        # ================================================================
        # 3. 初始化大语言模型
        # ================================================================
        # ChatTongyi 是 LangChain 封装的阿里云通义千问接口
        # 【关联】model 名称来自 config_data.py 的 config.chat_model_name
        # 【关联】dashscope_api_key 来自 config_data.py(联调例外配置,为空时自动读环境变量)
        self.chat_model = ChatTongyi(
            model=config.chat_model_name,   # "qwen3-max" — 通义千问旗舰模型
            dashscope_api_key=config.dashscope_api_key,  # 联调例外:代码配置 key
            streaming=True                  # 流式输出(对 invoke 无影响,返回完整结果)
        )

        # ================================================================
        # 4. 构建完整的 RAG 执行链
        # ================================================================
        self.chain = self.__get_chain()

    def ask(self, user_id: int, question: str) -> str:
        """
        AI 问答入口(供 main.py 的 /chat 接口调用)

        参数:
            user_id:  用户ID(来自 Java 后端从 token 解析的结果)
            question: 用户问题(1-500 字符)

        返回:
            AI 生成的回答字符串

        【关联】session_id 使用 str(user_id):
          每个用户对应一个独立的历史文件 chat_history/{userId},
          RunnableWithMessageHistory 会根据 session_id 自动加载/保存该用户的历史
        """
        session_config = {
            "configurable": {
                "session_id": str(user_id)  # 对应 file_history_store.py 中的历史文件名
            }
        }
        return self.chain.invoke({"input": question}, session_config)

    def __get_chain(self):
        """
        构建 RAG 执行链(核心方法)

        使用 LangChain 的 LCEL 声明式地编排整个 RAG 流水线:

        chain = (
            { 第一步:并行执行检索和输入透传 }
            {
                "input":   RunnablePassthrough(),           # 原样传递 input 字典
                "context": RunnableLambda(temp1)            # 提取 input["input"] 字符串
                           | retriever                      # 传给检索器获取相关文档
                           | format_document                # 将文档列表拼接成字符串
            }
            | 第二步:解包嵌套字典 }
            RunnableLambda(temp2)                           # 拆解嵌套的 input/history
            | 第三步:填入提示词模板 }
            self.prompt_template                            # 将 context/history/input 填入模板
            | 第四步:调用大模型 }
            self.chat_model                                 # 通义千问生成回答
            | 第五步:解析输出 }
            StrOutputParser()                               # 将 LLM 输出转为纯字符串
        )

        【为什么要用 temp1 和 temp2?】
        RunnableWithMessageHistory 会给 chain 传入 {"input": "用户问题", "history": [...]},
        但检索器 retriever 只能接收纯字符串,不能接收字典;
        而且经过第一层并行处理后,input 字段会变成嵌套结构,需要解包成平铺字典
        才能被提示词模板的占位符 {input}、{history}、{context} 正确替换。
        """
        # ----- 获取向量检索器 -----
        # 【关联】retriever 来自 vector_stores.py 的 VectorStoreService.get_retriever()
        #         接收一个字符串查询,返回 Document 对象列表
        retriever = self.vector_service.get_retriever()

        # 导入 LangChain 的 Document 类型(用于类型标注)
        from langchain_core.documents import Document

        def format_document(docs: list[Document]):
            """
            将检索到的文档列表格式化为提示词可用的字符串

            参数:
                docs: LangChain Document 对象列表(即使只有1个结果也是列表)

            返回:
                拼接好的字符串,例如:
                "文档片段: 青花瓷真伪鉴别方法...\n文档来源: 青花瓷真伪鉴别.txt\n"

            特殊处理:检索结果为空列表 [] 时返回"无相关参考资料",
            防止 LLM 在没有任何依据的情况下编造答案(幻觉)。

            【为什么不展示完整元数据字典?】
            元数据里的 operator、create_time 等内部字段会干扰 LLM 判断
            (实测曾导致 LLM 误把 operator 值当作回答依据)。
            只保留 source(来源文件名)供追溯。
            """
            if not docs:
                return "无相关参考资料"

            formatted_str = ""
            for doc in docs:
                # doc.page_content: 文档的文本正文
                # doc.metadata: 文档的元数据字典,这里只取 source 字段(来源文件名)
                formatted_str += f"文档片段: {doc.page_content}\n文档来源: {doc.metadata.get('source', '未知')}\n"

            return formatted_str

        def temp1(value: dict) -> str:
            """
            第一层转换函数:从 RunnableWithMessageHistory 传入的字典中提取用户问题字符串

            RunnableWithMessageHistory 传给 chain 的是 {"input": "用户问题", "history": [...]},
            但检索器 retriever 只能接收纯字符串,所以从中提取出 input 字段的值。

            参数:
                value: {"input": "用户问题", "history": [...]}

            返回:
                纯字符串 "用户问题"(传给 retriever 进行向量检索)
            """
            return value["input"]

        def temp2(value: dict) -> dict:
            """
            第二层转换函数:解包嵌套的字典结构

            经过第一层并行处理后,input 字段变成了嵌套结构:
            {
                "input":   {"input": "用户问题", "history": [...]},  ← 嵌套的!
                "context": "检索到的文档内容..."
            }
            但提示词模板期望的变量是平铺的 input、history、context,
            所以需要将嵌套的 input 和 history "拆出来"。

            参数:
                value: 嵌套的字典(并行步骤的输出)

            返回:
                平铺的字典 {"input": ..., "history": [...], "context": ...},
                正好对应提示词模板中的三个占位符
            """
            new_value = {}
            new_value["input"] = value["input"]["input"]     # 从嵌套中提取用户问题字符串
            new_value["context"] = value["context"]          # 保留检索结果(已经是格式化好的字符串)
            new_value["history"] = value["input"]["history"]  # 从嵌套中提取历史消息列表
            return new_value

        # ===== 构建 LCEL 链 =====
        # 每个 | 连接两个 Runnable 对象,前者的输出成为后者的输入
        # {"key": runnable, ...} 字典语法 = RunnableParallel(所有分支并行执行,结果按 key 合并)
        chain = (
            {
                "input": RunnablePassthrough(),                      # 原样传递完整的输入字典
                "context": RunnableLambda(temp1) | retriever | format_document  # 检索+格式化管道
            }
            | RunnableLambda(temp2)       # 第二步:解包嵌套字典 → 得到平铺的 {input, history, context}
            | self.prompt_template        # 第三步:填入提示词模板 → 占位符被替换为实际值
            | self.chat_model             # 第四步:调用通义千问大模型 → 返回 AIMessage
            | StrOutputParser()           # 第五步:将 AIMessage 解析为纯字符串
        )

        # ===== 包装多轮对话历史管理 =====
        # RunnableWithMessageHistory 是 LangChain 提供的包装器,自动完成三件事:
        #   1. 调用 get_history(session_id) 加载历史消息
        #   2. 在每次 invoke 后自动调用 add_messages() 保存新消息
        #   3. 将历史消息注入到 chain 的输入字典中(key 为 "history"),传给 chain
        # 参数说明:
        #   - input_messages_key:  输入中哪个字段包含用户消息("input")
        #   - history_messages_key: 提示词模板中历史记录的占位符名称("history")
        #                           【关联】必须与 prompt_template 中 MessagesPlaceholder("history")
        #                           的参数完全一致!否则历史消息无法正确注入
        conversation_chain = RunnableWithMessageHistory(
            chain,                         # 被包装的执行链
            get_history,                   # 工厂函数:session_id → FileChatMessageHistory 实例
            input_messages_key="input",    # chain 输入中用户消息的 key 名称
            history_messages_key="history"  # 提示词模板中历史占位符的名称(必须与 MessagesPlaceholder 一致!)
        )

        return conversation_chain

"""
=============================================================================
 config_data.py — AI 服务全局配置
=============================================================================
作用:集中管理 ai-side Python 服务的所有可配置参数。
     知识库目录、向量库路径、模型名称、文本分割参数、历史记录参数等,
     所有模块都从这里导入配置,修改一处即可全局生效。

【跨文件关联】
  - knowledge_base.py      → 入库(写):读 knowledge/*.txt → 向量化 → 存入 chroma_db
  - vector_stores.py       → 检索(读):从 chroma_db 查询与问题最相关的知识文档
  - rag_service.py         → 生成:检索 + 历史 + 通义千问,串联完整 RAG 链
  - file_history_store.py  → 历史:读写 chat_history/{userId}.json(本地文件)
  - main.py                → FastAPI 入口:POST /chat、GET /history

【关键约束】
  入库与检索必须使用同一个 collection_name、persist_directory 和 embedding 模型,
  否则向量空间不一致,检索结果会完全错误。
=============================================================================
"""

# ===========================
# 文件路径配置
# ===========================

# 知识库文档目录:存放古玩鉴赏知识文档(*.txt)
# 【使用者】knowledge_base.py → build_from_directory() 扫描此目录入库
knowledge_dir = "./knowledge"

# ChromaDB 向量数据库持久化存储目录
# 【使用者】knowledge_base.py → 写入向量数据
#          vector_stores.py  → 读取向量数据
# 【关键】两个模块必须指向同一个目录,否则"写入"和"读取"会操作不同的数据库
persist_directory = "./chroma_db"

# 聊天历史存储目录(每个用户一个 JSON 文件:chat_history/{userId})
# 【使用者】file_history_store.py → FileChatMessageHistory
chat_history_dir = "./chat_history"

# md5.text 文件路径,用于记录已入库文档的 MD5 哈希值(去重用)
# 【使用者】knowledge_base.py → check_md5() 读取, save_md5() 写入
md5_path = "./md5.text"

# ===========================
# ChromaDB 配置
# ===========================

# 向量数据库的集合(Collection)名称,相当于关系数据库中的"表名"
# 【关键】入库(knowledge_base.py)与检索(vector_stores.py)必须使用相同的值
collection_name = "ai_knowledge"

# ===========================
# 文本分割配置(RecursiveCharacterTextSplitter 参数)
# ===========================

# 每个文本块(chunk)的最大字符数
# 知识文档按段落和句子切成小块,方便向量化和精确检索
# 【使用者】knowledge_base.py → RecursiveCharacterTextSplitter(chunk_size=...)
chunk_size = 500

# 相邻文本块之间的重叠字符数,防止关键信息被切割在边界处
# 【使用者】knowledge_base.py → RecursiveCharacterTextSplitter(chunk_overlap=...)
chunk_overlap = 100

# 文本分割的分隔符优先级(从高到低):先按段落、再按行、再按标点、最后按字符
# 【使用者】knowledge_base.py → RecursiveCharacterTextSplitter(separators=...)
separators = ["\n\n", "\n", ".", "!", "?", " ", ""]

# 触发分割的最小字符数阈值:超过此长度的文本才会被分割,短文本直接整体入库
# 【使用者】knowledge_base.py → upload_by_str() 中 if len(data) > max_split_char_number
max_split_char_number = 300

# ===========================
# 检索参数配置
# ===========================

# 检索时返回的最相似文档数量(top-K)
# 值越大,给 LLM 的参考资料越多,回答更全面,但也会增加 token 消耗
# 【使用者】vector_stores.py → get_retriever() 中 search_kwargs={"k": ...}
similarity_threshold = 2

# ===========================
# 模型配置
# ===========================

# 阿里云 DashScope 的文本嵌入模型(知识入库与检索共用,必须一致)
embedding_model_name = "text-embedding-v4"

# 阿里云通义千问的对话模型
chat_model_name = "qwen3-max"

# ===========================
# API Key 配置(独立文件,联调例外)
# ===========================
# 【说明】团队联调时,组员只需修改 api_key.py 中的 key 即可,无需配置环境变量。
#         读取优先级:api_key.py(显式配置) > 环境变量 DASHSCOPE_API_KEY
#         - api_key.py 已被 .gitignore 忽略,不会上传云端仓库
#         - api_key.py 不存在时自动回退环境变量(安全最佳实践)
#         - 组员参考模板:api_key.example.py
# 【使用者】rag_service.py → ChatTongyi(dashscope_api_key=...)
#          knowledge_base.py → DashScopeEmbeddings(dashscope_api_key=...)
try:
    # 优先读取独立密钥文件(联调用,已被 git 忽略)
    from api_key import dashscope_api_key
except ImportError:
    # api_key.py 不存在 → 回退为 None
    # langchain 组件收到 None 时会自动读取环境变量 DASHSCOPE_API_KEY
    dashscope_api_key = None

# ===========================
# 历史记录配置
# ===========================

# GET /history 接口返回的最大消息条数(最近 N 条,按时间正序)
history_max_messages = 20

# ===========================
# 服务配置
# ===========================

# FastAPI 监听地址与端口(仅本机,Java 后端内网调用,不对外开放)
host = "127.0.0.1"
port = 8001

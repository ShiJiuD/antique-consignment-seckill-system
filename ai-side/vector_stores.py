"""
=============================================================================
 vector_stores.py — 向量数据库服务(检索引擎)
=============================================================================
作用:封装 ChromaDB 向量数据库,对外提供简洁的检索器(Retriever)接口。
     在 RAG 架构中,本模块负责"读"操作——根据用户问题检索最相关的知识文档片段。

【跨文件关联】与 knowledge_base.py 的读写分离架构
  knowledge_base.py(写)     vector_stores.py(读)
  ├─ 读 knowledge/*.txt     ├─ 查询向量化
  ├─ MD5 去重               ├─ 相似度计算(余弦相似度)
  ├─ 文本分割                ├─ Top-K 筛选
  ├─ 向量化 + 存入ChromaDB   ├─ 返回 Document 列表
  └─ 记录MD5                └─ 不做任何写入

  两者通过 config_data.py 中的相同配置关联:
    persist_directory = "./chroma_db"   ← 读写同一个目录
    collection_name = "ai_knowledge"    ← 读写同一个集合

关键约束:
  必须使用相同的 embedding_function,否则向量空间不一致,
  检索结果会完全错误——就像用中文词典查英文单词。

检索原理(向量相似度搜索):
  1. 文本被嵌入模型"翻译"为高维空间中的坐标点
  2. 语义相似的文本,坐标点的距离更近
  3. 所以"如何辨别青花瓷真伪"和"看胎釉、看青料发色"距离很近(语义相关)
=============================================================================
"""

from langchain_chroma import Chroma  # LangChain 封装的 ChromaDB 客户端
import config_data as config         # 【关联】所有配置参数(目录路径、集合名称、检索参数)


class VectorStoreService(object):
    """
    向量数据库服务类

    职责:初始化 ChromaDB 连接 + 提供检索器
    被 rag_service.py 中的 RagService 调用,用于从知识库中查找与用户问题相关的文档

    【与 knowledge_base.py 的对应关系】
    本类的方法 "读"  ←→  knowledge_base.py 的方法 "写"
    get_retriever()  ←→  KnowledgeBaseService.upload_by_str()
    """

    def __init__(self, embedding):
        """
        初始化向量数据库服务

        参数:
            embedding: 嵌入模型实例(例如 DashScopeEmbeddings)
                       负责将文本转换为向量(高维浮点数组)
                       【关联】必须与 knowledge_base.py 入库时使用的模型完全相同,
                       否则向量空间不一致,检索结果会出错
        """
        # 保存嵌入模型实例,供后续查询向量化使用
        self.embedding = embedding

        # 连接到 ChromaDB 向量数据库
        # Chroma 会自动检查 persist_directory 下是否存在已有的向量数据,
        # 如果目录为空,则创建一个空的集合(后续知识库入库时会填充)
        self.vector_store = Chroma(
            embedding_function=self.embedding,            # 查询时用哪个模型把问题转为向量
            persist_directory=config.persist_directory,   # 向量数据存储目录(与入库是同一个)
            collection_name=config.collection_name        # 集合名称(与入库是同一个)
        )

    def get_retriever(self):
        """
        获取 LangChain 检索器对象

        as_retriever() 将 ChromaDB 的向量搜索能力包装成标准的 LangChain Retriever 接口:
          输入: str(查询字符串)
          输出: List[Document](相关文档列表,按相似度降序排列)

        参数:
            search_kwargs={"k": config.similarity_threshold}  # top-K = 2(返回最相似的2个文档片段)

        返回:
            LangChain Retriever 对象,可直接嵌入 RAG 链中使用
        """
        return self.vector_store.as_retriever(
            search_kwargs={"k": config.similarity_threshold}
        )

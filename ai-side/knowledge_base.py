"""
=============================================================================
 knowledge_base.py — 知识库管理服务(知识入库的入口)
=============================================================================
作用:管理 RAG 知识库的"写入"操作。将 knowledge/ 目录下的古玩知识文档经过
     MD5 去重 → 文本分割 → 向量化 → 存入 ChromaDB。
     这是整个 AI 服务的知识来源——只有入库的文档才能被检索和用于生成回答。

核心功能:
  1. MD5 去重:相同内容的文档不会重复入库,节省存储和计算资源
  2. 文本分割:长文档按语义边界切成小块(默认 ≤500 字符/块),提高检索精度
  3. 向量化存储:将文本块转为向量存入 ChromaDB,附带元数据(来源、时间)

【跨文件关联】读写分离架构
  knowledge_base.py(写)   →  ChromaDB(./chroma_db,集合 ai_knowledge)  ←  vector_stores.py(读)

使用方式(入库):
  python knowledge_base.py
  将扫描 knowledge/ 目录下所有 *.txt 文档并入库(已入库的自动跳过)

外部依赖:
  - 阿里云 DashScope text-embedding-v4:文本向量化模型
  - ChromaDB:向量数据库
  - md5.text:去重记录文件(由本模块管理)
=============================================================================
"""

import os
import hashlib
import datetime

from langchain_chroma import Chroma  # LangChain 封装的 ChromaDB 客户端
from langchain_community.embeddings import DashScopeEmbeddings  # 阿里云嵌入模型
from langchain_text_splitters import RecursiveCharacterTextSplitter  # 递归文本分割器

import config_data as config  # 【关联】所有配置参数从这里导入


# ===========================
# MD5 去重相关函数
# ===========================

def check_md5(md5_str: str):
    """
    检查传入的 MD5 值是否已经存在于去重记录中

    【为什么用 MD5 去重而不是文件名去重?】
    MD5 是"内容指纹":只要内容相同,MD5 就相同,和文件名无关。
    这样即使用户把文档改名后再次入库,也能正确识别并跳过。

    参数:
        md5_str: 文档内容的 MD5 哈希值(32位十六进制字符串)

    返回:
        True:  该文档已入库,需要跳过
        False: 该文档未入库,可以继续处理
    """
    if not os.path.exists(config.md5_path):
        # md5.text 不存在 → 还没有任何文档入过库,创建空文件并返回 False
        open(config.md5_path, "w", encoding="utf-8").close()
        return False
    else:
        # 逐行读取已有的 MD5 记录,strip() 去除行尾换行符后比较
        for line in open(config.md5_path, "r", encoding="utf-8").readlines():
            if line.strip() == md5_str:
                return True   # 找到匹配 → 文档已存在,跳过
        return False          # 遍历完都没找到 → 新文档,可以入库


def save_md5(md5_str: str):
    """
    将新的 MD5 值追加写入去重记录文件(追加模式,"a" 模式不会覆盖已有内容)

    参数:
        md5_str: 文档内容的 MD5 哈希值
    """
    open(config.md5_path, "a", encoding="utf-8").write(md5_str + "\n")


def get_string_md5(input_str: str, encoding="utf-8"):
    """
    计算输入字符串的 MD5 哈希值(文档内容的"数字指纹")

    MD5 的特点:确定性(相同内容→相同MD5)、雪崩效应(内容稍变→MD5大变)

    参数:
        input_str: 要计算哈希的字符串

    返回:
        32位十六进制 MD5 字符串(如 "7eca689f0d3389d9dea66ae112e5cfd7")
    """
    byte_str = input_str.encode(encoding)  # 字符串 → 字节数组(hashlib 只接受 bytes)
    hash_obj = hashlib.md5()
    hash_obj.update(byte_str)
    return hash_obj.hexdigest()


# ===========================
# 知识库管理服务类
# ===========================

class KnowledgeBaseService(object):
    """
    知识库管理服务类

    职责:接收文本文档,将其处理并存入向量数据库。

    入库流程(完整流水线):
      文本字符串 → get_string_md5() → check_md5() →
      (超过阈值则 RecursiveCharacterTextSplitter 分割) →
      DashScopeEmbeddings 向量化 → Chroma.add_texts() 存入 →
      save_md5() 记录
    """

    def __init__(self):
        """
        初始化知识库服务

        主要做两件事:
          1. 连接 ChromaDB 向量数据库(用于存储向量化文档)
          2. 初始化文本分割器(用于将长文档切成小块)

        【关键】collection_name、persist_directory、embedding 模型
        必须与 vector_stores.py 的 VectorStoreService 完全一致
        (都来自 config_data.py),否则写入的数据检索不到。
        """
        # 确保向量库存储目录存在
        os.makedirs(config.persist_directory, exist_ok=True)

        # 初始化 ChromaDB 向量数据库连接
        self.chroma = Chroma(
            collection_name=config.collection_name,          # 集合名称(相当于"表名")
            # 阿里云嵌入模型;dashscope_api_key 来自 config_data.py(联调例外配置,为空时自动读环境变量)
            embedding_function=DashScopeEmbeddings(
                model=config.embedding_model_name,
                dashscope_api_key=config.dashscope_api_key
            ),
            persist_directory=config.persist_directory       # 存储路径
        )

        # 初始化递归字符文本分割器
        # 按 separators 的优先级逐级尝试分割:段落 → 换行 → 标点 → 字符,
        # 尽量在"语义边界"处断开,而不是粗暴地在固定字数处切断
        self.spliter = RecursiveCharacterTextSplitter(
            chunk_size=config.chunk_size,          # 每块最大字符数
            chunk_overlap=config.chunk_overlap,    # 相邻块重叠字符数(防止关键信息被边界切断)
            separators=config.separators,          # 分隔符优先级列表
            length_function=len                    # 用 len() 计算字符串长度
        )

    def upload_by_str(self, data: str, filename):
        """
        将文本字符串上传到知识库(核心方法)

        完整入库流程(6步):
          Step 1: 计算文本的 MD5 哈希值
          Step 2: 检查是否已入库(去重)
          Step 3: 判断文本长度,决定是否需要分割
          Step 4: 为每个文本块创建元数据(来源、入库时间)
          Step 5: 批量写入 ChromaDB 向量数据库
          Step 6: 保存 MD5 值到去重记录

        参数:
            data:     要入库的文本内容(完整的文档字符串)
            filename: 原始文件名(用于元数据记录来源)

        返回:
            状态字符串:"[跳过]内容已经存在知识库中" 或 "[成功]内容已添加到知识库"
        """
        # Step 1: 计算文本的 MD5 "数字指纹"
        md5_hex = get_string_md5(data)

        # Step 2: MD5 去重检查
        if check_md5(md5_hex):
            return "[跳过]内容已经存在知识库中"

        # Step 3: 文本分割(如果需要的话)
        if len(data) > config.max_split_char_number:
            knowledge_chunks = self.spliter.split_text(data)  # 返回字符串列表(文本块)
        else:
            # 文本较短,直接整体作为一个块(add_texts 需要列表参数)
            knowledge_chunks = [data]

        # Step 4: 创建元数据(每个文本块附带相同的一份)
        metadata = {
            "source": filename,                                                              # 来源文件名
            "create_time": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),            # 入库时间
            "operator": "ai-side"                                                            # 操作人标识
        }

        # Step 5: 批量写入向量数据库
        # add_texts() 内部会自动:文本向量化(调用 embedding) → 向量 + 文本 + 元数据 一起存入 ChromaDB
        # 【关联】存入后,vector_stores.py 的 retriever 就能检索到这些数据
        self.chroma.add_texts(
            knowledge_chunks,                              # 文本块列表
            metadatas=[metadata for _ in knowledge_chunks]  # 与 texts 一一对应的元数据列表
        )

        # Step 6: 记录 MD5,防止下次重复入库
        save_md5(md5_hex)

        return "[成功]内容已添加到知识库"

    def build_from_directory(self):
        """
        将 knowledge/ 目录下的所有 *.txt 知识文档批量入库

        这是 AI 服务的知识初始化入口:启动服务前先执行一次。
        已入库的文档(MD5 相同)自动跳过,重复执行安全。

        返回:
            list[str]: 每个文档的处理结果(成功/跳过)
        """
        results = []
        if not os.path.exists(config.knowledge_dir):
            print(f"[错误]知识库目录不存在: {config.knowledge_dir}")
            return results

        for filename in sorted(os.listdir(config.knowledge_dir)):
            # 只处理 .txt 文档(按文件名排序,入库顺序稳定)
            if not filename.endswith(".txt"):
                continue

            file_path = os.path.join(config.knowledge_dir, filename)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()

            if not content.strip():
                print(f"[跳过]{filename}: 文件内容为空")
                continue

            result = self.upload_by_str(content, filename)
            print(f"{result} → {filename}")
            results.append(f"{filename}: {result}")

        return results


# ===========================
# 独立入库入口
# ===========================
if __name__ == '__main__':
    # 批量入库知识库文档:python knowledge_base.py
    print("=" * 40)
    print("开始知识库入库:")
    print(f"  知识目录: {config.knowledge_dir}")
    print(f"  向量目录: {config.persist_directory}")
    print(f"  集合名称: {config.collection_name}")
    print(f"  嵌入模型: {config.embedding_model_name}")
    print("=" * 40)

    service = KnowledgeBaseService()
    results = service.build_from_directory()

    print("=" * 40)
    print(f"入库完成,共处理 {len(results)} 个文档")
    print("=" * 40)

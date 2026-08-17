"""
=============================================================================
 app_file_uploader.py — 知识库更新上传界面(Streamlit,移植自桌面 RAG 项目)
=============================================================================
作用:使用 Streamlit 框架构建的文件上传 Web 界面。
     管理员通过浏览器上传 .txt 知识文档,系统自动将其向量化并存入 ChromaDB,
     新文档立刻可以被 AI 助手检索到——无需重启服务、无需改代码。

界面功能:
  1. 顶部标题 "古玩知识库更新服务"
  2. 文件上传区域:支持选择本地 .txt 文件
  3. 文件信息展示:显示文件名、格式、大小
  4. 上传状态反馈:显示处理结果(成功 / 跳过 / 错误)

启动方式:
  streamlit run app_file_uploader.py

使用流程:
  1. 打开网页 → 看到上传区域
  2. 点击 "Browse files" 选择一个 .txt 文件(如 knowledge/青花瓷真伪鉴别.txt)
  3. 系统读取文件内容 → 计算MD5去重 → 文本分割 → 向量化 → 存入ChromaDB
  4. 页面显示上传结果

【跨文件关联】调用链
  app_file_uploader.py(本文件)
    ├─ st.file_uploader() → 用户选择 .txt 文件
    ├─ uploaded_file.getvalue().decode("utf-8") → 读取文本内容
    └─ knowledge_base.py → KnowledgeBaseService.upload_by_str(text, filename)
        ├─ get_string_md5() → MD5 哈希值
        ├─ check_md5()      → 去重检查(读取 md5.text)
        ├─ spliter.split_text() → 文本分割
        ├─ chroma.add_texts()   → 向量化 + 存入 ChromaDB
        └─ save_md5()           → 记录 MD5

  数据写入 ChromaDB 后:
    rag_service.py 通过 vector_stores.py 读取同一个 ChromaDB,
    使用相同的 collection_name 和 persist_directory(都来自 config_data.py)
    → 新上传的知识立刻可以被 AI 问答检索到

注意事项:
  - 只支持 .txt 格式文件(UTF-8 编码,非 UTF-8 文件可能乱码)
  - 相同内容的文档不会重复入库(MD5 去重机制)
  - streamlit 每次交互都会重新运行整个脚本,session_state 用于保持状态
=============================================================================
"""

import sys
import os
import time

# 将当前脚本所在目录添加到 Python 模块搜索路径
# 【为什么需要这行?】Streamlit 运行时的工作目录可能不是脚本所在目录,
# 不加这行可能导致 `from knowledge_base import KnowledgeBaseService` 失败
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import streamlit as st
# 【关联】导入 knowledge_base.py 中的知识库服务类
#        KnowledgeBaseService 负责文档去重、分割、向量化、存储
from knowledge_base import KnowledgeBaseService


# ===========================
# 页面标题
# ===========================
st.title("古玩知识库更新服务")


# ===========================
# 文件上传组件
# ===========================
# st.file_uploader() 创建浏览器原生的文件上传区域
# 参数说明:
#   type=["txt"]:                 只接受 .txt 格式
#   accept_multiple_files=False:  单文件上传模式(一次一个)
uploaded_file = st.file_uploader(
    "请上传 .txt 知识文档",
    type=["txt"],                   # 限制文件类型为 .txt
    accept_multiple_files=False     # 单文件模式
)


# ===========================
# 初始化知识库服务(保持在 session_state 中)
# ===========================
# KnowledgeBaseService 初始化时会连接 ChromaDB 和创建文本分割器,
# 初始化开销较大,所以只做一次,后续交互复用
# 【关联】KnowledgeBaseService 定义在 knowledge_base.py 中
if "service" not in st.session_state:
    st.session_state["service"] = KnowledgeBaseService()


# ===========================
# 处理文件上传
# ===========================
# uploaded_file 不为 None 表示用户已经选择了文件
if uploaded_file is not None:
    # ---- 步骤1:读取文件基本信息 ----
    file_name = uploaded_file.name          # 文件名(如 "青花瓷真伪鉴别.txt")
    file_type = uploaded_file.type          # MIME类型(如 "text/plain")
    file_size = uploaded_file.size / 1024   # 文件大小(字节转KB)

    # ---- 步骤2:在页面上显示文件信息 ----
    st.subheader(f"文件名: {file_name}")
    st.write(f"格式: {file_type} | 大小: {file_size: .2f} KB")

    # ---- 步骤3:读取文件文本内容 ----
    # uploaded_file.getvalue() 返回文件的原始字节数据(bytes 类型)
    # .decode("utf-8") 将字节数据解码为 UTF-8 字符串
    # 【注意】如果文件不是 UTF-8 编码(如 GBK),这里会抛出 UnicodeDecodeError
    text = uploaded_file.getvalue().decode("utf-8")

    # ---- 步骤4:上传到知识库(带加载动画) ----
    # 向量化需要调用阿里云 API(网络请求),可能耗时几秒
    with st.spinner("上传中..."):
        time.sleep(1)  # 短暂延迟,让用户看到上传动画(纯体验优化,可移除)
        # 调用 KnowledgeBaseService.upload_by_str() 完成完整的入库流程
        # 返回值是状态字符串:
        #   "[成功]内容已添加到知识库"   — 入库成功
        #   "[跳过]内容已经存在知识库中"  — MD5去重拦截
        result = st.session_state["service"].upload_by_str(text, file_name)

        # ---- 步骤5:显示处理结果 ----
        st.write(result)

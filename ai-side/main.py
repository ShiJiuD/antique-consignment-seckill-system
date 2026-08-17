"""
=============================================================================
 main.py — AI 服务入口(FastAPI)
=============================================================================
作用:对外提供 HTTP 接口,供 Java 后端(antique-server)内网调用。
     前端不直接访问本服务,所有请求统一走 Java 后端转发。

提供两个内部接口:
  POST /chat              {userId, question} → {code, msg, data:{answer}}
  GET  /history?userId=   → {code, msg, data:{list:[{role, content}]}}

启动方式:
  uvicorn main:app --host 127.0.0.1 --port 8001

【部署注意】
  1. 首次启动前先执行知识入库:python knowledge_base.py
     (将 knowledge/ 目录下的古玩知识文档向量化存入 chroma_db)
  2. 环境变量 DASHSCOPE_API_KEY 必须已配置(通义千问 + 向量模型调用凭证)
  3. 仅监听 127.0.0.1,不对公网开放;Java 后端通过 application.yml 的
     ai.python.url 配置访问本服务

【响应格式与 Java 后端统一】
  {code: 1=成功 0=失败, msg: 提示信息, data: 业务数据}
=============================================================================
"""

import os

from fastapi import FastAPI
from pydantic import BaseModel, Field

import config_data as config
from rag_service import RagService
from file_history_store import history_to_api_list

# ===========================
# 全局初始化
# ===========================

# 创建全局唯一的 RAG 服务实例(内部持有向量库连接、通义千问模型、完整执行链)
# 单例模式:所有请求复用同一个链,避免重复加载模型和连接
rag_service = RagService()

# 创建 FastAPI 应用
app = FastAPI(
    title="古玩寄卖平台 AI 助手服务",
    description="内部服务:仅 Java 后端(antique-server)调用,不对外开放",
    version="1.0.0"
)


# ===========================
# 请求/响应模型
# ===========================

class ChatRequest(BaseModel):
    """AI 问答请求体(POST /chat)"""
    userId: int = Field(..., description="用户ID(Java 从 token 解析后传入)")
    question: str = Field(..., min_length=1, max_length=500, description="用户问题(1-500字符)")


# ===========================
# 接口定义
# ===========================

@app.post("/chat")
def chat(req: ChatRequest):
    """
    AI 问答:检索知识库 → 组装提示词(含历史)→ 通义千问生成 → 保存历史

    参数:
        req: {userId, question}

    返回:
        {"code": 1, "msg": "success", "data": {"answer": "..."}}
        失败时:{"code": 0, "msg": "AI服务暂时不可用,请稍后重试"}
    """
    try:
        answer = rag_service.ask(req.userId, req.question)
        return {"code": 1, "msg": "success", "data": {"answer": answer}}
    except Exception as e:
        # 兜底:任何异常(网络、限流、模型报错)都不让服务崩掉,
        # 统一返回可读的失败信息,由 Java 后端转发给前端
        import traceback
        traceback.print_exc()
        return {"code": 0, "msg": "AI服务暂时不可用,请稍后重试"}


@app.get("/history")
def history(userId: int):
    """
    获取用户最近的对话历史(按时间正序,最多 config.history_max_messages 条)

    参数:
        userId: 用户ID(query 参数)

    返回:
        {"code": 1, "msg": "success", "data": {"list": [{"role": "user"/"assistant", "content": "..."}]}}
        历史为空(新用户)→ list 为空数组
    """
    try:
        messages = history_to_api_list(str(userId), config.history_max_messages)
        return {"code": 1, "msg": "success", "data": {"list": messages}}
    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"code": 0, "msg": "获取历史失败", "data": {"list": []}}


# ===========================
# 启动检查
# ===========================

# 启动时检查向量库是否已初始化,提醒先执行知识入库
if not os.path.exists(config.persist_directory):
    print("=" * 50)
    print("[提示] 向量库目录不存在,AI 将无法检索到知识文档!")
    print("       请先执行: python knowledge_base.py")
    print("       将 knowledge/ 目录下的古玩知识文档入库后重启服务")
    print("=" * 50)


# ===========================
# 直接运行入口
# ===========================
if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host=config.host, port=config.port)

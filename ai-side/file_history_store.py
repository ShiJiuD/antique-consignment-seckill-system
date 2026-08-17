"""
=============================================================================
 file_history_store.py — 聊天历史记录存储服务(基于本地 JSON 文件)
=============================================================================
作用:实现 LangChain 的 BaseChatMessageHistory 接口,将多轮对话历史持久化到本地 JSON 文件。
     每个用户(userId)对应一个独立的聊天记录文件,让 AI 记住和该用户聊过的内容。

核心机制:
  1. 每个 session_id 对应一个聊天记录文件(如 chat_history/1、chat_history/2)
     【关联】session_id 由 main.py 传入的 userId 转换而来:str(userId)
  2. 文件内容为 JSON 数组,每条消息是 {"type": "human"/"ai", "data": {...}} 格式
  3. 使用 LangChain 官方的 message_to_dict() / messages_from_dict() 序列化
  4. 每次对话后全量覆写文件(简单可靠,适合单机小规模场景)

文件格式示例(chat_history/1):
  [
    {"type": "human", "data": {"content": "如何辨别青花瓷真伪", ...}},
    {"type": "ai",    "data": {"content": "辨别青花瓷真伪可以从...", ...}}
  ]

【为什么用 JSON 数组而非对象?】
  messages_from_dict() 的参数类型是 List[dict],
  数组存储可一步到位:json.load(f) → list[dict] → messages_from_dict() → List[BaseMessage]

历史读写链路:
  main.py /chat → rag_service.py → RunnableWithMessageHistory
    → get_history(session_id) → FileChatMessageHistory
    → 读取 chat_history/{session_id} → 注入提示词 → LLM 生成 → 追加保存
=============================================================================
"""

import json
import os
from typing import Sequence

from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage
from langchain_core.messages import message_to_dict
from langchain_core.messages import messages_from_dict

import config_data as config


def get_history(session_id):
    """
    聊天历史工厂函数(供 LangChain RunnableWithMessageHistory 调用)

    LangChain 的 RunnableWithMessageHistory 需要在每次对话时,根据 session_id
    动态创建不同的历史记录对象,所以要求传入"工厂函数"而非固定的历史对象。

    参数:
        session_id: 会话ID(由 userId 转换而来,如 "1"),每个用户有独立的历史文件

    返回:
        FileChatMessageHistory 实例,绑定到 chat_history 目录下的对应文件
    """
    return FileChatMessageHistory(session_id, config.chat_history_dir)


def history_to_api_list(session_id: str, max_count: int = 20) -> list:
    """
    将本地历史文件转换为前端展示格式 [{role, content}, ...]

    供 main.py 的 GET /history 接口调用。

    角色映射:
      LangChain 的 "human" → 前端 "user"(用户提问)
      LangChain 的 "ai"    → 前端 "assistant"(AI 回答)

    参数:
        session_id: 会话ID(即 userId 的字符串形式)
        max_count:  返回的最大消息条数(取最近 N 条)

    返回:
        list[dict],按时间正序(最早的在前),最多 max_count 条;
        历史文件不存在或损坏时返回空列表
    """
    history = FileChatMessageHistory(session_id, config.chat_history_dir)
    messages = list(history.messages)

    result = []
    for msg in messages[-max_count:]:
        # msg.type: BaseMessage 的类型标识,"human"=用户消息,"ai"=AI消息
        role = "user" if msg.type == "human" else "assistant"
        # content 可能是纯字符串;若不是(如多模态消息),转成字符串兜底
        content = msg.content if isinstance(msg.content, str) else str(msg.content)
        result.append({"role": role, "content": content})
    return result


class FileChatMessageHistory(BaseChatMessageHistory):
    """
    基于 JSON 文件的聊天历史记录管理类

    继承自 LangChain 的 BaseChatMessageHistory,与其生态无缝集成。
    所有消息以 JSON 数组格式存储在本地文件中,服务重启后不会丢失。

    必须实现三个接口:
      - messages(属性):读取历史
      - add_messages(方法):保存消息
      - clear(方法):清空历史
    """

    def __init__(self, session_id, storage_path):
        """
        初始化聊天历史记录管理器

        参数:
            session_id:   会话ID(用作文件名,如 "1")
            storage_path: 存储目录路径(如 "./chat_history")

        初始化逻辑(三步):
          1. 拼接完整文件路径 → ./chat_history/1
          2. 确保存储目录存在(不存在则递归创建)
          3. 如果历史文件不存在或为空,写入空数组 [](防止后续 json.load 报错)
        """
        self.session_id = session_id           # 会话ID,同时也是文件名
        self.storage_path = storage_path       # 历史文件存放的文件夹路径
        self.file_path = os.path.join(self.storage_path, self.session_id)

        # 确保文件夹存在(exist_ok=True:已存在时不报错)
        os.makedirs(os.path.dirname(self.file_path), exist_ok=True)

        # 初始化空历史文件:写入 [] 而非 {} 是因为 messages 属性中
        # 调用 messages_from_dict(json.load(f)),json.load 读取 [] 返回 list,
        # 正好匹配 messages_from_dict 的参数类型 List[dict]
        if not os.path.exists(self.file_path) or os.path.getsize(self.file_path) == 0:
            with open(self.file_path, "w", encoding="utf-8") as f:
                json.dump([], f)

    def add_messages(self, messages: Sequence[BaseMessage]) -> None:
        """
        追加新消息到聊天历史中(全量覆写模式)

        RunnableWithMessageHistory 在每次问答完成后自动调用此方法,
        保存用户问题和 AI 回复。

        策略:读取全部旧消息 → 追加新消息 → 全量覆写回文件
        优点:简单可靠;缺点:消息极多时效率较低(聊天场景完全够用)
        """
        # 步骤1:读取文件中已有的所有历史消息
        all_messages = list(self.messages)

        # 步骤2:将新消息追加到列表末尾
        all_messages.extend(messages)

        # 步骤3:将所有 Message 对象转为可 JSON 序列化的字典列表
        #        message_to_dict() 将 BaseMessage → {"type": "human"/"ai", "data": {...}}
        new_messages = [message_to_dict(message) for message in all_messages]

        # 步骤4:全量覆写写入文件
        # ensure_ascii=False:不将中文转成 \uXXXX 转义序列,保证文件可读
        with open(self.file_path, "w", encoding="utf-8") as f:
            json.dump(new_messages, f, ensure_ascii=False)

    @property
    def messages(self) -> list[BaseMessage]:
        """
        读取聊天历史记录(属性方式访问)

        LangChain 的 BaseChatMessageHistory 接口要求 messages 是属性而非方法。

        数据流:json.load(f) → list[dict] → messages_from_dict() → list[BaseMessage]

        异常处理:文件被意外删除或内容损坏时返回空列表,不抛异常,
        避免历史文件问题导致整个问答链路中断。
        """
        try:
            with open(self.file_path, "r", encoding="utf-8") as f:
                messages_data = json.load(f)  # 返回 list[dict]
                return messages_from_dict(messages_data)  # 反序列化为 Message 对象列表
        except (FileNotFoundError, json.JSONDecodeError):
            return []

    def clear(self) -> None:
        """
        清空当前会话的所有聊天历史(重置为空数组 [])
        """
        with open(self.file_path, "w", encoding="utf-8") as f:
            json.dump([], f)

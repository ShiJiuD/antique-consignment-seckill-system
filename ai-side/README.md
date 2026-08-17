# ai-side — 古玩寄卖平台 AI 助手服务(Python)

智能知识库问答的 Python 后端,**改造自桌面 RAG 项目**(langchain + ChromaDB + 阿里云通义千问)。
由 Java 后端(`antique-server`)内网调用,前端不直接访问本服务。

## 架构

```
前端(Vue) → Java 后端 /api/ai/** → 本服务(127.0.0.1:8001) → 通义千问 qwen3-max
                                     ├── ChromaDB 知识库检索(text-embedding-v4)
                                     └── 对话历史:chat_history/{userId}.json(本地文件)
```

## 目录结构

| 文件/目录 | 说明 |
|---|---|
| `knowledge/` | 古玩知识文档(*.txt),AI 回答的知识依据 |
| `chroma_db/` | 向量数据库(运行生成,已 gitignore) |
| `chat_history/` | 对话历史,每用户一个文件(运行生成,已 gitignore) |
| `config_data.py` | 全局配置(路径、模型名、检索参数) |
| `knowledge_base.py` | 知识入库脚本:knowledge/*.txt → 向量化 → chroma_db |
| `vector_stores.py` | 向量检索服务(读) |
| `file_history_store.py` | 对话历史存储(本地 JSON 文件,一用户一文件) |
| `rag_service.py` | RAG 执行链(检索 + 历史 + 通义千问) |
| `main.py` | FastAPI 入口 |

## 启动步骤

1. **创建虚拟环境并安装依赖**(首次)
   ```
   python -m venv .venv
   ```
   > 注:若报 `No module named venv`(某些 Python 发行版),改用:
   > ```
   > pip install virtualenv && python -m virtualenv .venv
   > ```

   ```
   .venv\Scripts\python.exe -m pip install -r requirements.txt
   ```

2. **配置 API Key**(二选一)
   - **方式一(联调推荐)**:复制 `api_key.example.py` 为 `api_key.py`,填入自己的
     `DASHSCOPE_API_KEY`(该文件已被 git 忽略,不会上传云端仓库)
   - **方式二**:配置环境变量 `DASHSCOPE_API_KEY`(通义千问 + 向量模型调用凭证)
   - 优先级:api_key.py > 环境变量

3. **知识入库**(首次启动,或修改 knowledge/ 文档后重新执行;已入库的自动跳过)
   ```
   cd ai-side
   .venv\Scripts\python.exe knowledge_base.py
   ```

4. **启动服务**
   ```
   .venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8001
   ```

5. **启动 Java 后端**(antique-server,须先启动 MySQL 和 Redis)
   ```
   cd server-side
   mvn -pl antique-server spring-boot:run
   ```

## 接口(仅 Java 后端调用,不对外开放)

| 接口 | 说明 |
|---|---|
| `POST /chat` | 请求 `{userId, question}` → 返回 `{code, msg, data:{answer}}` |
| `GET /history?userId=` | 返回最近 20 条历史 `{code, msg, data:{list:[{role, content}]}}` |

## 知识库更新

编辑 `knowledge/` 下的文档,或通过上传界面(`app_file_uploader.py`)添加新文档后:

1. 重新执行第 3 步入库(`python knowledge_base.py`,相同内容自动跳过)
2. **重启 AI 服务**(Ctrl+C 后重新执行第 4 步)—— 向量索引在服务启动时加载,
   运行中更新知识库不会自动生效,重启后才能检索到新知识

> 文档内容**变更**时,建议先删除旧版本片段再入库(否则新旧两个版本会同时存在):
> 删除 `chroma_db/` 和 `md5.text` 后重新执行入库,是最简单可靠的全量重建方式。

文档入库、检索必须使用同一嵌入模型(`text-embedding-v4`),
否则向量空间不一致会导致检索失败。

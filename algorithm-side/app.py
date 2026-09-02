from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
import time
import traceback
from typing import Optional
from validator import validate_input
from predictor import predict
from logger import logger
import config

app = FastAPI(title="商品预测服务", description="预测未来7天访问量和订单量")

@app.get("/health")
async def health():
    return {"status": "ok"}

@app.post("/predict")
async def predict_endpoint(request: Request):
    try:
        data = await request.json()
    except Exception:
        return JSONResponse(
            status_code=400,
            content={"code": 0, "msg": "请求体必须是合法的 JSON", "data": None}
        )

    # 记录请求时间
    request_time = time.strftime("%Y-%m-%d %H:%M:%S")
    antique_id = data.get("antiqueId", "unknown")

    # 输入校验
    is_valid, error_msg, clean_data = validate_input(data)
    if not is_valid:
        logger.warning(f"请求校验失败 - 时间: {request_time}, antiqueId: {antique_id}, 错误: {error_msg}")
        return JSONResponse(
            status_code=400,
            content={"code": 0, "msg": error_msg, "data": None}
        )

    # 提取数据
    history_visit = clean_data["historyVisit"]
    history_order = clean_data["historyOrder"]

    # 计算输入摘要（均值）
    visit_mean = sum(history_visit) / len(history_visit)
    order_mean = sum(history_order) / len(history_order)

    # 执行预测
    try:
        result = predict(
            history_visit,
            history_order,
            use_exp_smooth=config.USE_EXPONENTIAL_SMOOTHING
        )
        predict_visit = result["predict_visit"]
        predict_order = result["predict_order"]
        algorithm = result["algorithm"]
    except Exception as e:
        # 理论上 predict 内部已经捕获异常，但以防万一
        logger.error(f"预测过程发生未捕获异常，时间: {request_time}, antiqueId: {antique_id}, 错误: {e}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"code": 0, "msg": "模型计算失败，请稍后重试", "data": None}
        )

    # 记录日志
    logger.info(
        f"请求时间: {request_time}, antiqueId: {antique_id}, "
        f"输入均值: visit={visit_mean:.2f}, order={order_mean:.2f}, "
        f"使用算法: {algorithm}, "
        f"预测输出: visit={predict_visit}, order={predict_order}"
    )

    # 返回成功响应
    return {
        "code": 1,
        "msg": "success",
        "data": {
            "predict_visit": predict_visit,
            "predict_order": predict_order
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=config.HOST, port=config.PORT)
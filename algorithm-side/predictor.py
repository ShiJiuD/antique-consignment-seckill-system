from typing import List
import logging
from logger import logger

def _round_non_negative(value: float) -> int:
    """四舍五入取整，负数截断为0"""
    rounded = int(round(value))
    return max(0, rounded)

def simple_moving_average(history: List[int], forecast_horizon: int = 7) -> List[int]:
    """简单移动平均：取历史均值重复 forecast_horizon 次"""
    avg = sum(history) / len(history)
    return [_round_non_negative(avg) for _ in range(forecast_horizon)]

def exponential_smoothing(history: List[int], forecast_horizon: int = 7, alpha: float = 0.5) -> List[int]:
    """
    一次指数平滑法：根据最后值进行平滑预测，所有未来值相同。
    公式：S_t = alpha * y_t + (1-alpha) * S_{t-1}
    这里用历史最后一个值作为 S_0，然后逐步平滑得到 S_last，未来预测均为 S_last。
    """
    if not history:
        raise ValueError("历史数据为空，无法进行指数平滑")
    s = history[0]
    for y in history[1:]:
        s = alpha * y + (1 - alpha) * s
    # 未来预测值均为平滑后的最后一个值
    return [_round_non_negative(s) for _ in range(forecast_horizon)]

def predict(history_visit: List[int], history_order: List[int], use_exp_smooth: bool = False) -> dict:
    """
    核心预测函数，返回包含预测结果和算法信息的字典
    """
    algorithm_used = "移动平均"
    try:
        if use_exp_smooth:
            # 尝试指数平滑
            predict_visit = exponential_smoothing(history_visit)
            predict_order = exponential_smoothing(history_order)
            algorithm_used = "指数平滑"
        else:
            # 直接使用移动平均
            predict_visit = simple_moving_average(history_visit)
            predict_order = simple_moving_average(history_order)
    except Exception as e:
        # 任何异常降级为移动平均
        logger.error(f"指数平滑失败，降级为移动平均。错误: {e}", exc_info=True)
        predict_visit = simple_moving_average(history_visit)
        predict_order = simple_moving_average(history_order)
        algorithm_used = "降级移动平均"

    return {
        "predict_visit": predict_visit,
        "predict_order": predict_order,
        "algorithm": algorithm_used
    }
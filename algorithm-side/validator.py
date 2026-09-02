from typing import List, Any, Tuple, Optional

def validate_input(data: dict) -> Tuple[bool, Optional[str], Optional[dict]]:
    """
    校验输入数据，返回 (是否合法, 错误消息, 清洗后的数据)
    """
    if not isinstance(data, dict):
        return False, "请求体必须是 JSON 对象", None

    antique_id = data.get("antiqueId")
    if antique_id is None:
        return False, "缺少 antiqueId 字段", None

    history_visit = data.get("historyVisit")
    history_order = data.get("historyOrder")

    if history_visit is None or history_order is None:
        return False, "历史数据不能为空", None

    if not isinstance(history_visit, list) or not isinstance(history_order, list):
        return False, "historyVisit 与 historyOrder 必须为数组", None

    if len(history_visit) != 7 or len(history_order) != 7:
        return False, "historyVisit 与 historyOrder 必须各为 7 个元素", None

    # 检查每个元素是否为整数且非负
    for i, val in enumerate(history_visit):
        if not isinstance(val, int) or isinstance(val, bool):
            return False, f"historyVisit[{i}] 必须是整数", None
        if val < 0:
            return False, f"historyVisit[{i}] 不能为负数", None

    for i, val in enumerate(history_order):
        if not isinstance(val, int) or isinstance(val, bool):
            return False, f"historyOrder[{i}] 必须是整数", None
        if val < 0:
            return False, f"historyOrder[{i}] 不能为负数", None

    clean_data = {
        "antiqueId": antique_id,
        "historyVisit": history_visit,
        "historyOrder": history_order,
    }
    return True, None, clean_data
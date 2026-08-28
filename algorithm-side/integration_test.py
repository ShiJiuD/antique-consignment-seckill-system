# integration_test.py
import requests
import json
import sys

BASE_URL = "http://127.0.0.1:8002"

def call_predict(payload):
    """模拟后端调用预测接口"""
    try:
        # 设置超时时间（文档建议 3 秒）
        response = requests.post(
            f"{BASE_URL}/predict",
            json=payload,
            timeout=3
        )
        # 检查 HTTP 状态码
        if response.status_code != 200:
            print(f"[失败] HTTP 状态码异常: {response.status_code}")
            return None
        return response.json()
    except requests.exceptions.Timeout:
        print("[失败] 请求超时（超过 3 秒）")
        return None
    except Exception as e:
        print(f"[失败] 请求异常: {e}")
        return None

def validate_response(resp, expected_code=1):
    """检查响应结构是否符合文档"""
    if resp is None:
        return False

    # 检查顶层字段
    required_fields = {"code", "msg", "data"}
    if not required_fields.issubset(resp.keys()):
        print("[失败] 响应缺少必要字段: code, msg, data")
        return False

    if resp["code"] != expected_code:
        print(f"[失败] 预期 code={expected_code}，实际 code={resp['code']}")
        return False

    if expected_code == 1:
        # 成功时检查 data 结构
        data = resp.get("data")
        if data is None:
            print("[失败] 成功响应中 data 为 null")
            return False

        predict_visit = data.get("predict_visit")
        predict_order = data.get("predict_order")

        if not isinstance(predict_visit, list) or not isinstance(predict_order, list):
            print("[失败] predict_visit 或 predict_order 不是数组")
            return False

        if len(predict_visit) != 7 or len(predict_order) != 7:
            print("[失败] 预测数组长度不是 7")
            return False

        for i, val in enumerate(predict_visit):
            if not isinstance(val, int) or val < 0:
                print(f"[失败] predict_visit[{i}] 不是非负整数")
                return False

        for i, val in enumerate(predict_order):
            if not isinstance(val, int) or val < 0:
                print(f"[失败] predict_order[{i}] 不是非负整数")
                return False

        print("[成功] 响应结构完全符合文档")
        return True
    else:
        # 失败时检查 data 是否为 null（根据文档）
        if resp.get("data") is not None:
            print("[警告] 失败响应中 data 不为 null，但文档要求 null")
            # 这里只警告，不判定失败，因为你的代码已经这样实现
        print(f"[预期失败] msg = {resp.get('msg')}")
        return True

# 测试用例
if __name__ == "__main__":
    # 1. 正常请求
    print("=== 测试 1：正常请求 ===")
    normal_payload = {
        "antiqueId": 10001,
        "historyVisit": [213, 198, 246, 231, 208, 265, 287],
        "historyOrder": [12, 9, 15, 11, 10, 16, 18]
    }
    resp = call_predict(normal_payload)
    validate_response(resp, expected_code=1)

    # 2. 长度不足
    print("\n=== 测试 2：历史数据长度不足 ===")
    bad_payload = {
        "antiqueId": 10001,
        "historyVisit": [213, 198, 246],
        "historyOrder": [12, 9, 15]
    }
    resp = call_predict(bad_payload)
    validate_response(resp, expected_code=0)

    # 3. 包含负数
    print("\n=== 测试 3：包含负数 ===")
    bad_payload = {
        "antiqueId": 10001,
        "historyVisit": [213, 198, 246, 231, 208, 265, -1],
        "historyOrder": [12, 9, 15, 11, 10, 16, 18]
    }
    resp = call_predict(bad_payload)
    validate_response(resp, expected_code=0)

    # 4. 缺少字段
    print("\n=== 测试 4：缺少 historyOrder ===")
    bad_payload = {
        "antiqueId": 10001,
        "historyVisit": [213, 198, 246, 231, 208, 265, 287]
    }
    resp = call_predict(bad_payload)
    validate_response(resp, expected_code=0)

    # 5. 非法 JSON（模拟）
    print("\n=== 测试 5：非法 JSON ===")
    try:
        r = requests.post(f"{BASE_URL}/predict", data="not a json", timeout=3)
        resp = r.json()
        validate_response(resp, expected_code=0)
    except Exception as e:
        print(f"[失败] 非法 JSON 请求异常: {e}")
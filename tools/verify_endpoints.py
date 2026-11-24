#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""springboot-admin-demo 端到端验证。

前置：admin-server(8080) 与 admin-client(8081) 均已启动。
覆盖：业务接口、Actuator 端点、日志动态调级、注册、健康状态 UP<->DOWN 链路。
"""
import json
import time
import urllib.error
import urllib.request

SERVER = "http://localhost:8080"
CLIENT = "http://localhost:8081"

PASS = 0
FAIL = 0


def request(method, url, body=None, content_type="application/json", accept="application/json"):
    headers = {"Accept": accept}
    data = None
    if body is not None:
        data = body.encode("utf-8") if isinstance(body, str) else json.dumps(body).encode("utf-8")
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode("utf-8", "replace")
            return r.status, raw
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001
        return -1, str(e)


def check(name, ok, detail=""):
    global PASS, FAIL
    if ok:
        PASS += 1
        print("[PASS] %s" % name)
    else:
        FAIL += 1
        print("[FAIL] %s  %s" % (name, detail))


def main():
    # ---------- Server ----------
    s, b = request("GET", SERVER + "/actuator/health")
    check("Server health -> 200", s == 200, "status=%s" % s)

    s, b = request("GET", SERVER + "/", accept="text/html")
    check("Server Admin UI 首页可达", s == 200 and "<title>spring boot admin</title>" in b.lower(), "status=%s" % s)

    # ---------- Client 业务接口 ----------
    s, b = request("POST", CLIENT + "/api/orders", {"product": "机械键盘", "quantity": 2})
    check("创建订单 -> 200", s == 200, "status=%s body=%s" % (s, b[:200]))

    s, b = request("POST", CLIENT + "/api/orders", {"product": "", "quantity": 0})
    check("非法参数 -> 400", s == 400, "status=%s body=%s" % (s, b[:200]))

    s, b = request("GET", CLIENT + "/api/orders/999")
    check("订单不存在 -> 404", s == 404, "status=%s" % s)

    s, b = request("GET", CLIENT + "/api/orders")
    ok = s == 200 and json.loads(b).get("data")
    check("订单列表 -> 200 且有数据", ok, "status=%s body=%s" % (s, b[:200]))

    # ---------- Client Actuator ----------
    s, b = request("GET", CLIENT + "/actuator/health")
    health = json.loads(b) if s == 200 else {}
    check("Client health 整体 UP", s == 200 and health.get("status") == "UP", "status=%s" % s)
    comps = health.get("components", {})
    check("health 含自定义指示器 payment-channel", "payment-channel" in comps,
          "components=%s" % sorted(comps.keys()))
    check("health 展示组件详情（show-details=always）",
          isinstance(comps.get("diskSpace"), dict) and "details" in comps.get("diskSpace", {}),
          "diskSpace=%s" % comps.get("diskSpace"))

    for path in ("env", "beans", "mappings", "conditions", "configprops", "loggers", "metrics", "threaddump"):
        s, _ = request("GET", CLIENT + "/actuator/" + path)
        check("Client /actuator/%s -> 200" % path, s == 200, "status=%s" % s)

    s, b = request("GET", CLIENT + "/actuator/info")
    info = json.loads(b) if s == 200 else {}
    check("info 端点暴露应用信息", "app" in info, "body=%s" % b[:200])

    # ---------- 日志动态调级 ----------
    LOGGER = "com.example.adminclient.business"
    s, _ = request("POST", CLIENT + "/actuator/loggers/" + LOGGER, {"configuredLevel": "DEBUG"})
    check("动态调级 INFO -> DEBUG（POST loggers）", s in (200, 204), "status=%s（204 为 Actuator 标准成功响应）" % s)
    s, b = request("GET", CLIENT + "/actuator/loggers/" + LOGGER)
    lvl = json.loads(b).get("configuredLevel") if s == 200 else None
    check("确认新级别已生效", lvl == "DEBUG", "configuredLevel=%s" % lvl)
    s, b = request("GET", CLIENT + "/api/demo/log?level=debug")
    check("触发 DEBUG 日志输出", s == 200, "status=%s" % s)
    s, _ = request("POST", CLIENT + "/actuator/loggers/" + LOGGER, {"configuredLevel": "INFO"})
    s, b = request("GET", CLIENT + "/actuator/loggers/" + LOGGER)
    lvl = json.loads(b).get("configuredLevel") if s == 200 else None
    check("恢复为 INFO", lvl == "INFO", "configuredLevel=%s" % lvl)

    # ---------- 注册验证 ----------
    s, b = request("GET", SERVER + "/instances")
    instances = json.loads(b) if s == 200 else []
    names = [i.get("registration", {}).get("name") for i in instances]
    check("Client 已注册到 Server", "admin-client" in names, "instances=%s" % names)
    reg = next((i for i in instances if i.get("registration", {}).get("name") == "admin-client"), None)
    status = (reg or {}).get("statusInfo", {}).get("status")
    check("Server 视角实例状态 UP", status == "UP", "statusInfo.status=%s" % status)

    # ---------- 健康状态 UP -> DOWN -> UP ----------
    s, b = request("POST", CLIENT + "/api/demo/dependency?healthy=false")
    check("模拟依赖异常 -> 200", s == 200, "status=%s" % s)
    s, b = request("GET", CLIENT + "/actuator/health")
    # 健康为 DOWN 时 HTTP 状态是 503，但 body 仍是完整 JSON
    try:
        health = json.loads(b)
    except Exception:  # noqa: BLE001
        health = {}
    check("Client 健康变为 DOWN（HTTP 503 + body DOWN）", s == 503 and health.get("status") == "DOWN",
          "http=%s body.status=%s" % (s, health.get("status")))

    server_down = False
    for _ in range(40):  # Server 轮询周期默认约 10s，最多等 60s
        s, b = request("GET", SERVER + "/instances")
        instances = json.loads(b) if s == 200 else []
        reg = next((i for i in instances if i.get("registration", {}).get("name") == "admin-client"), None)
        if reg and reg.get("statusInfo", {}).get("status") == "DOWN":
            server_down = True
            break
        time.sleep(1.5)
    check("Server 视角实例状态跟随变为 DOWN", server_down, "60s 内未观测到 DOWN")

    s, b = request("POST", CLIENT + "/api/demo/dependency?healthy=true")
    check("恢复依赖正常 -> 200", s == 200, "status=%s" % s)
    s, b = request("GET", CLIENT + "/actuator/health")
    health = json.loads(b) if s == 200 else {}
    check("Client 健康恢复 UP", health.get("status") == "UP", "status字段=%s" % health.get("status"))

    server_up = False
    for _ in range(40):
        s, b = request("GET", SERVER + "/instances")
        instances = json.loads(b) if s == 200 else []
        reg = next((i for i in instances if i.get("registration", {}).get("name") == "admin-client"), None)
        if reg and reg.get("statusInfo", {}).get("status") == "UP":
            server_up = True
            break
        time.sleep(1.5)
    check("Server 视角实例状态恢复 UP", server_up, "60s 内未观测到 UP")

    print("\nTOTAL=%d PASS=%d FAIL=%d" % (PASS + FAIL, PASS, FAIL))
    return 0 if FAIL == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())

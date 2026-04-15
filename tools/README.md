# 天气查询 CLI 工具

基于和风天气 API 的命令行天气查询工具。

## 快速开始

### 1. 配置 API KEY

在项目根目录创建 `.env` 文件（或设置环境变量）：

```bash
# 和风天气 API 配置
QWEATHER_API_KEY=your_api_key_here
QWEATHER_API_HOST=mr487tqjdj.re.qweatherapi.com
```

### 2. 测试 CLI 工具

```bash
# 方式 1：使用环境变量
export QWEATHER_API_KEY=your_api_key_here
node tools/weather-cli.js 北京

# 方式 2：直接修改 weather-cli.js 中的 API_KEY
node tools/weather-cli.js 杭州
```

### 3. 预期输出

```json
{
  "success": true,
  "city": "北京",
  "updateTime": "2026-04-14T10:30+08:00",
  "weather": {
    "temp": "18",
    "feelsLike": "17",
    "text": "晴",
    "windDir": "西南风",
    "windScale": "3",
    "humidity": "45",
    "precip": "0.0",
    "pressure": "1015",
    "vis": "30"
  },
  "summary": "北京当前天气：晴，温度18℃（体感17℃），西南风3级，湿度45%"
}
```

## 支持的城市

目前支持以下城市（可在 `weather-cli.js` 中扩展）：

- 北京、上海、杭州、深圳、广州
- 成都、西安、武汉、南京、重庆
- 天津、苏州、郑州、长沙、沈阳
- 青岛、宁波、厦门

## 错误处理

### 错误示例 1：缺少城市参数

```bash
$ node tools/weather-cli.js
{
  "success": false,
  "error": "缺少城市参数",
  "usage": "node weather-cli.js <城市名>"
}
```

### 错误示例 2：不支持的城市

```bash
$ node tools/weather-cli.js 拉萨
{
  "success": false,
  "error": "不支持的城市: 拉萨",
  "supportedCities": ["北京", "上海", "杭州", ...]
}
```

### 错误示例 3：API KEY 错误

```bash
{
  "success": false,
  "error": "API 错误: 401",
  "message": "认证失败，可能使用了错误的KEY、数字签名错误、KEY的类型错误"
}
```

## 和风天气 API 文档

- 官方文档：https://dev.qweather.com/docs/api/
- 实时天气 API：https://dev.qweather.com/docs/api/weather/weather-now/
- 城市查询：https://dev.qweather.com/docs/api/geoapi/

## 添加更多城市

在 `weather-cli.js` 的 `CITY_MAP` 中添加：

```javascript
const CITY_MAP = {
  '拉萨': '101140101',  // 在和风天气官网查询城市 ID
  // ...
};
```

城市 ID 查询方法：
1. 访问 https://github.com/qwd/LocationList/blob/master/China-City-List-latest.csv
2. 搜索城市名称，获取对应的 Location ID

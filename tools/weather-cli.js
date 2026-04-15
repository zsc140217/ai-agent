#!/usr/bin/env node

/**
 * 天气查询 CLI 工具
 * 使用和风天气 API 查询指定城市的实时天气
 *
 * 使用方法：
 *   node weather-cli.js <城市名>
 *
 * 示例：
 *   node weather-cli.js 北京
 *   node weather-cli.js 杭州
 */

const https = require('https');
const zlib = require('zlib');

// 从环境变量读取配置
const API_KEY = process.env.QWEATHER_API_KEY || 'YOUR_API_KEY';
const API_HOST = process.env.QWEATHER_API_HOST || 'mr487tqjdj.re.qweatherapi.com';

// 城市名称到 location ID 的映射（和风天气需要 location ID）
const CITY_MAP = {
  '北京': '101010100',
  '上海': '101020100',
  '杭州': '101210101',
  '深圳': '101280601',
  '广州': '101280101',
  '成都': '101270101',
  '西安': '101110101',
  '武汉': '101200101',
  '南京': '101190101',
  '重庆': '101040100',
  '天津': '101030100',
  '苏州': '101190401',
  '郑州': '101180101',
  '长沙': '101250101',
  '沈阳': '101070101',
  '青岛': '101120201',
  '宁波': '101210401',
  '厦门': '101230201'
};

function getCityLocation(cityName) {
  // 移除"市"后缀
  const cleanName = cityName.replace(/市$/, '');
  return CITY_MAP[cleanName] || null;
}

function queryWeather(location) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: API_HOST,
      path: `/v7/weather/now?location=${location}`,
      method: 'GET',
      headers: {
        'X-QW-Api-Key': API_KEY,
        'Accept-Encoding': 'gzip, deflate'
      }
    };

    const req = https.request(options, (res) => {
      let chunks = [];

      // 处理 gzip 压缩
      let stream = res;
      if (res.headers['content-encoding'] === 'gzip') {
        stream = res.pipe(zlib.createGunzip());
      } else if (res.headers['content-encoding'] === 'deflate') {
        stream = res.pipe(zlib.createInflate());
      }

      stream.on('data', (chunk) => {
        chunks.push(chunk);
      });

      stream.on('end', () => {
        try {
          const data = Buffer.concat(chunks).toString('utf-8');
          const result = JSON.parse(data);
          resolve(result);
        } catch (e) {
          reject(new Error('解析响应失败: ' + e.message));
        }
      });

      stream.on('error', (e) => {
        reject(new Error('解压缩失败: ' + e.message));
      });
    });

    req.on('error', (e) => {
      reject(new Error('请求失败: ' + e.message));
    });

    req.end();
  });
}

async function main() {
  // 检查参数
  if (process.argv.length < 3) {
    console.error(JSON.stringify({
      success: false,
      error: '缺少城市参数',
      usage: 'node weather-cli.js <城市名>'
    }));
    process.exit(1);
  }

  const cityName = process.argv[2];
  const location = getCityLocation(cityName);

  if (!location) {
    console.error(JSON.stringify({
      success: false,
      error: `不支持的城市: ${cityName}`,
      supportedCities: Object.keys(CITY_MAP)
    }));
    process.exit(1);
  }

  try {
    const result = await queryWeather(location);

    // 调试：打印完整响应
    // console.error('API Response:', JSON.stringify(result, null, 2));

    if (result.code === '200') {
      // 成功获取天气数据
      const weather = result.now;
      const output = {
        success: true,
        city: cityName,
        updateTime: result.updateTime,
        weather: {
          temp: weather.temp,
          feelsLike: weather.feelsLike,
          text: weather.text,
          windDir: weather.windDir,
          windScale: weather.windScale,
          humidity: weather.humidity,
          precip: weather.precip,
          pressure: weather.pressure,
          vis: weather.vis
        },
        summary: `${cityName}当前天气：${weather.text}，温度${weather.temp}℃（体感${weather.feelsLike}℃），${weather.windDir}${weather.windScale}级，湿度${weather.humidity}%`
      };
      console.log(JSON.stringify(output, null, 2));
    } else {
      // API 返回错误 - 打印完整响应以便调试
      console.error(JSON.stringify({
        success: false,
        error: `API 错误: ${result.code}`,
        message: getErrorMessage(result.code),
        fullResponse: result
      }));
      process.exit(1);
    }
  } catch (error) {
    console.error(JSON.stringify({
      success: false,
      error: error.message
    }));
    process.exit(1);
  }
}

function getErrorMessage(code) {
  const errorMap = {
    '204': '请求成功，但你查询的地区暂时没有你需要的数据',
    '400': '请求错误，可能包含错误的请求参数或缺少必选的请求参数',
    '401': '认证失败，可能使用了错误的KEY、数字签名错误、KEY的类型错误',
    '402': '超过访问次数或余额不足以支持继续访问服务',
    '403': '无访问权限，可能是绑定的PackageName、BundleID、域名IP地址不一致',
    '404': '查询的数据或地区不存在',
    '429': '超过限定的QPM',
    '500': '无响应或超时'
  };
  return errorMap[code] || '未知错误';
}

// 运行主函数
main();

package org.qiunet.data.core.support.redis;

import org.qiunet.utils.data.IKeyValueData;
import org.qiunet.utils.exceptions.CustomException;
import org.qiunet.utils.json.JsonUtil;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.string.StringUtil;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Callable;

public abstract class BaseRedisUtil implements IRedisUtil {
	 static final Class [] JEDIS_INTERFACES = new Class[]{IJedis.class};
	protected static final Logger logger = LoggerType.DUODUO_REDIS.getLogger();

	private final String redisName;

	 BaseRedisUtil(String redisName) {
		this.redisName = redisName;
	}

	/**
	 * 池配置
	 * @param redisConfig
	 * @return
	 */
	JedisPoolConfig buildPoolConfig(IKeyValueData<String, String> redisConfig) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMinIdle(redisConfig.getInt(getConfigKey("minIdle"), 1));
		poolConfig.setMaxIdle(redisConfig.getInt(getConfigKey("maxIdle"), 5));
		poolConfig.setMaxTotal(redisConfig.getInt(getConfigKey("maxTotal"), 30));
		poolConfig.setTestWhileIdle(redisConfig.getBoolean(getConfigKey("testWhileIdle")));
		poolConfig.setMaxWait(Duration.ofMillis(redisConfig.getInt(getConfigKey("maxWait"), 3000)));
		poolConfig.setNumTestsPerEvictionRun(redisConfig.getInt(getConfigKey("numTestsPerEvictionRun"), 30));
		poolConfig.setMinEvictableIdleTime(Duration.ofMillis(redisConfig.getInt(getConfigKey("minEvictableIdleTime"), 60000)));
		poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(redisConfig.getInt(getConfigKey("timeBetweenEvictionRuns"), 60000)));
		return poolConfig;
	}
	/**
	 * Redis 锁.
	 * @param key
	 * @return
	 */
	@Override
	public RedisLock redisLock(String key){
		return new RedisLock(this, key);
	}

	/**
	 * 拼接类似: redis.{redisName}.host 的字符串
	 * @param originConfigKey
	 * @return
	 */
	 String getConfigKey(String originConfigKey) {
		return "redis."+redisName+"."+originConfigKey;
	}

	@Override
	public <R> R redisLockRun(String key, Callable<R> call) throws IOException {
		try (RedisLock lock = redisLock(key)) {
			if (lock.lock()) {
				return call.call();
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException("call redis lock run exception: ", e);
		}
		return null;
	}

	/**
	 * 打印命令
	 * @param method
	 * @param args
	 * @param result
	 * @param startDt
	 */
	 static void logCommand(Method method, Object[] args, Object result, long startDt) {
		 long endDt = System.currentTimeMillis();

		 StringBuilder sb = new StringBuilder();
		 sb.append("Command[").append(String.format("%-8s", method.getName())).append("]").append(String.format("%2s", (endDt-startDt))).append("ms Key[").append(args[0]).append("] ");
		 if (args.length > 1) sb.append("\tParams:").append(StringUtil.arraysToString(args, "[", "]", 1, args.length - 1, ",")).append(" ");
		 sb.append("\tResult[");
		 if (result == null) {
			 sb.append("<null>");
		 }else {
			 sb.append(JsonUtil.toJsonString(result));
		 }
		 sb.append("]");
		 logger.info(sb.toString());
	 }


}

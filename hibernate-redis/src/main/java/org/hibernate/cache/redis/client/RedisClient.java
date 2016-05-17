package org.hibernate.cache.redis.client;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.redisson.core.RMapCache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RedisClient implemented using Redisson library
 * <p>
 * see https://github.com/mrniko/redisson
 *
 * @author debop sunghyouk.bae@gmail.com
 */
@Slf4j
public class RedisClient {

  public static final int DEFAULT_EXPIRY_IN_SECONDS = 120;
  public static final String DEFAULT_REGION_NAME = "hibernate";

  private final RedissonClient redisson;

  @Getter
  @Setter
  private int expiryInSeconds;

  public RedisClient() {
    this(Redisson.create());
  }

  public RedisClient(RedissonClient redisson) {
    this(redisson, DEFAULT_EXPIRY_IN_SECONDS);
  }

  @SneakyThrows
  public RedisClient(@NonNull RedissonClient redisson, int expiryInSeconds) {
    log.debug("RedisClient created. config={}, expiryInSeconds={}", redisson.getConfig().toJSON(), expiryInSeconds);
    this.redisson = redisson;

    if (expiryInSeconds >= 0) {
      this.expiryInSeconds = expiryInSeconds;
    }
  }

  public long dbSize() {
    return redisson.getKeys().count();
  }

  public boolean exists(final String region, final Object key) {
    return getCache(region).containsKey(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String region, final Object key) {
    return (T) getCache(region).get(key);
  }

  public boolean isExpired(final String region, final Object key) {
    return exists(region, key);
  }

//  public Set<Object> keysInRegion(final String region) {
//    return getCache(region).keySet();
//  }

  public long keySizeInRegion(final String region) {
    return getCache(region).size();
  }

  public Map<Object, Object> hgetAll(final String region) {
    return getCache(region);
  }

  public void set(final String region, final Object key, Object value) {
    set(region, key, value, expiryInSeconds);
  }

  public void set(final String region, final Object key, Object value, final long timeoutInSeconds) {
    set(region, key, value, timeoutInSeconds, TimeUnit.SECONDS);
  }

  public void set(final String region, final Object key, Object value, final long timeout, final TimeUnit unit) {
    RMapCache<Object, Object> cache = redisson.getMapCache(region);
    if (timeout > 0L) {
      cache.put(key, value, timeout, unit);
    } else {
      cache.put(key, value);
    }
  }

  public void expire(final String region) {
    getCache(region).clearExpire();
  }

  public void del(final String region, final Object key) {
    getCache(region).fastRemove(key);
  }

  public void mdel(final String region, final Collection<?> keys) {
    getCache(region).fastRemove(keys.toArray(new Object[keys.size()]));
  }

  public void deleteRegion(final String region) {
    getCache(region).clear();
  }

  public void flushDb() {
    log.info("flush db...");
    redisson.getKeys().flushdb();
  }

  public boolean isShutdown() {
    return redisson.isShutdown();
  }

  public void shutdown() {
    redisson.shutdown();
  }

  private RMapCache<Object, Object> getCache(final String region) {
    return redisson.getMapCache(region);
  }
}

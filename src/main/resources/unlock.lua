-- 比较当前 Redis 中的值是否和传进来的唯一标识（ARGV[1]）一致
if redis.call('get', KEYS[1]) == ARGV[1] then
    -- 如果一致，说明是自己的锁，执行删除并返回 1
    return redis.call('del', KEYS[1])
else
    -- 如果不一致，说明锁已过期或被别人占用，直接返回 0
    return 0
end

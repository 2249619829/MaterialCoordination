local _M = {}

local limit_rate = require "resty.limit.rate"

function _M.client_ip()
  local forwarded_for = ngx.var.http_x_forwarded_for
  if forwarded_for and forwarded_for ~= "" then
    local first_ip = forwarded_for:match("([^,%s]+)")
    if first_ip and first_ip ~= "" then
      return first_ip
    end
  end

  if ngx.var.http_x_real_ip and ngx.var.http_x_real_ip ~= "" then
    return ngx.var.http_x_real_ip
  end

  return ngx.var.remote_addr or "unknown"
end

function _M.limit(key, replenish_rate, burst_capacity, requested_tokens)
  requested_tokens = requested_tokens or 1

  local limiter, err = limit_rate.new("token_bucket_store", 1000, burst_capacity, replenish_rate, 0, {
    lock_enable = true,
    locks_shdict_name = "token_bucket_locks",
  })
  if not limiter then
    ngx.log(ngx.ERR, "failed to create token bucket limiter: ", err)
    return ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
  end

  local delay
  delay, err = limiter:take(key, requested_tokens, true)
  if not delay then
    if err == "rejected" then
      local retry_after = math.max(1, math.ceil(requested_tokens / replenish_rate))
    ngx.status = ngx.HTTP_TOO_MANY_REQUESTS
    ngx.header["Retry-After"] = tostring(retry_after)
    ngx.header["Content-Type"] = "application/json; charset=utf-8"
    ngx.say('{"code":429,"message":"请求太频繁，请稍后再试","data":null}')
    return ngx.exit(ngx.HTTP_TOO_MANY_REQUESTS)
  end
    ngx.log(ngx.ERR, "failed to take token: ", err)
    return ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
  end

  if delay >= 0.001 then
    ngx.sleep(delay)
  end
  return true
end

function _M.limit_ip(prefix, replenish_rate, burst_capacity, requested_tokens)
  return _M.limit(prefix .. ":" .. _M.client_ip(), replenish_rate, burst_capacity, requested_tokens)
end

return _M

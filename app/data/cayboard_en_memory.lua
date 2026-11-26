-- Keep dictionary English words manually pushed back by the user.
local DEMOTED_FILE = "/cayboard_en_demoted.txt"
local demoted_cache = { key = nil, words = {} }

-- Lowercase words the user pushed back; re-read when the file changes (size plus its last bytes).
local function load_demoted()
    local dir = rime_api and rime_api.get_user_data_dir and rime_api.get_user_data_dir()
    if not dir then return {} end
    local f = io.open(dir .. DEMOTED_FILE, "r")
    if not f then
        demoted_cache.key, demoted_cache.words = nil, {}
        return demoted_cache.words
    end
    local size = f:seek("end") or 0
    f:seek("set", math.max(0, size - 64))
    local key = size .. ":" .. (f:read("*a") or "")
    if key ~= demoted_cache.key then
        f:seek("set", 0)
        local words = {}
        for line in f:lines() do
            local w = line:match("^%s*(%a+)%s*$")
            if w then words[w:lower()] = true end
        end
        demoted_cache.key, demoted_cache.words = key, words
    end
    f:close()
    return demoted_cache.words
end

-- Pushed-back dictionary English words (lowercase input only) wait until four other candidates have gone out; when
-- the input itself is such a word (mean), its English completions (means, meaning) wait too, or one of them led.
local function cayboard_en_memory(input, env)
    local code = env.engine.context.input
    local demoted = (code:match("^%l+$") and env.engine.schema.schema_id ~= "cayboard_english") and load_demoted() or nil
    if not demoted or next(demoted) == nil then
        for cand in input:iter() do yield(cand) end
        return
    end
    local held, emitted = {}, 0
    local typed_demoted = demoted[code] == true
    local function emit(cand)
        local lower = cand.text:match("^%a+$") and cand.text:lower()
        if emitted < 4 and lower and (demoted[lower] or (typed_demoted and lower:sub(1, #code) == code)) then
            held[#held + 1] = cand
            return
        end
        yield(cand)
        emitted = emitted + 1
        if emitted == 4 then
            for _, h in ipairs(held) do yield(h) end
            held = {}
        end
    end
    for cand in input:iter() do emit(cand) end
    for _, h in ipairs(held) do yield(h) end
end

return cayboard_en_memory

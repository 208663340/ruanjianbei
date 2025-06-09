/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.xunzhi.common.constant;

/**
 * 短链接后管 Redis 缓存常量类
 
 */
public class RedisCacheConstant {

    /**
     * 用户注册分布式锁
     */
    public static final String LOCK_USER_REGISTER_KEY = "xunzhi-agent:lock_user-register:";

    /**
     * 分组创建分布式锁
     */
    public static final String LOCK_GROUP_CREATE_KEY = "xunzhi-agent:lock_group-create:%s";

    /**
     * 用户登录缓存标识
     */
    public static final String USER_LOGIN_KEY = "xunzhi-agent:login:";

    /**
     * 会话消息缓存标识
     */
    public static final String SESSION_MESSAGES_KEY = "xunzhi-agent:session:messages:";

    /**
     * 会话同步队列标识
     */
    public static final String SESSION_SYNC_QUEUE_KEY = "xunzhi-agent:session:sync:queue";

    /**
     * 会话最后同步时间标识
     */
    public static final String SESSION_LAST_SYNC_KEY = "xunzhi-agent:session:last-sync:";
}

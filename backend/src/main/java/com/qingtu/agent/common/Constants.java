package com.qingtu.agent.common;

/**
 * 系统常量定义类
 * 
 * 包含所有全局常量，避免魔法值
 * 
 * @author 青途智伴技术团队
 */
public class Constants {

    // ==================== 用户相关 ====================
    
    /**
     * 默认头像
     */
    public static final String DEFAULT_AVATAR = "https://qingtu-ai.oss-cn-beijing.aliyuncs.com/default/avatar.png";
    
    /**
     * 密码加密盐值
     */
    public static final String PASSWORD_SALT = "qingtu_secret_salt_2024";
    
    /**
     * Token有效期（7天）
     */
    public static final long TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    // ==================== 健康档案相关 ====================
    
    /**
     * BMI正常范围最小值
     */
    public static final double BMI_NORMAL_MIN = 18.5;
    
    /**
     * BMI正常范围最大值
     */
    public static final double BMI_NORMAL_MAX = 24.0;
    
    /**
     * 饮食目标：减脂
     */
    public static final String DIET_GOAL_LOSE = "lose";
    
    /**
     * 饮食目标：增肌
     */
    public static final String DIET_GOAL_GAIN = "gain";
    
    /**
     * 饮食目标：均衡
     */
    public static final String DIET_GOAL_BALANCE = "balance";
    
    /**
     * 每日基础代谢卡路里（默认）
     */
    public static final int DEFAULT_BMR = 1500;

    // ==================== 消费分类 ====================
    
    /**
     * 消费分类：饮食
     */
    public static final String COST_CATEGORY_FOOD = "food";
    
    /**
     * 消费分类：交通
     */
    public static final String COST_CATEGORY_TRANSPORT = "transport";
    
    /**
     * 消费分类：娱乐
     */
    public static final String COST_CATEGORY_ENTERTAINMENT = "entertainment";
    
    /**
     * 消费分类：购物
     */
    public static final String COST_CATEGORY_SHOPPING = "shopping";
    
    /**
     * 消费分类：生活
     */
    public static final String COST_CATEGORY_LIFE = "life";
    
    /**
     * 消费分类：学习
     */
    public static final String COST_CATEGORY_STUDY = "study";
    
    /**
     * 消费分类：其他
     */
    public static final String COST_CATEGORY_OTHER = "other";
    
    /**
     * 账单来源：微信
     */
    public static final String COST_SOURCE_WECHAT = "wechat";
    
    /**
     * 账单来源：支付宝
     */
    public static final String COST_SOURCE_ALIPAY = "alipay";
    
    /**
     * 账单来源：手动
     */
    public static final String COST_SOURCE_MANUAL = "manual";

    // ==================== 课程相关 ====================
    
    /**
     * 课程分类：必修
     */
    public static final String COURSE_TYPE_REQUIRED = "required";
    
    /**
     * 课程分类：选修
     */
    public static final String COURSE_TYPE_ELECTIVE = "elective";
    
    /**
     * 课程分类：公选
     */
    public static final String COURSE_TYPE_PUBLIC = "public";
    
    /**
     * 教学周数（默认）
     */
    public static final int DEFAULT_TEACHING_WEEKS = 16;
    
    /**
     * 学期开始日期（默认）
     */
public static final String DEFAULT_SEMESTER_START = "2024-02-26";

    // ==================== 知识库类型 ====================

    /**
     * 知识库类型：通用知识（全局共用）
     */
    public static final String KNOWLEDGE_TYPE_COMMON = "common";

    /**
     * 知识库类型：学校专属
     */
    public static final String KNOWLEDGE_TYPE_SCHOOL = "school";

    /**
     * 知识库类型：联网搜索
     */
    public static final String KNOWLEDGE_TYPE_WEB = "web";

    /**
     * 知识库版本：最新
     */
    public static final String KNOWLEDGE_VERSION_LATEST = "latest";

    // ==================== RAG知识库相关 ====================
    
    /**
     * 知识库分类：课程大纲
     */
    public static final String KNOWLEDGE_CATEGORY_COURSE = "course";
    
    /**
     * 知识库分类：考点资料
     */
    public static final String KNOWLEDGE_CATEGORY_EXAM = "exam";
    
    /**
     * 知识库分类：食堂档案
     */
    public static final String KNOWLEDGE_CATEGORY_CANTeen = "canteen";
    
    /**
     * 知识库分类：校园生活
     */
    public static final String KNOWLEDGE_CATEGORY_CAMPUS = "campus";
    
    /**
     * 文本切片大小（字符数）
     */
    public static final int TEXT_CHUNK_SIZE = 500;
    
    /**
     * 文本切片重叠大小（字符数）
     */
    public static final int TEXT_CHUNK_OVERLAP = 50;
    
    /**
     * 相似度阈值
     */
    public static final double SIMILARITY_THRESHOLD = 0.7;

    // ==================== 定时任务相关 ====================
    
    /**
     * 任务Key：早安推送
     */
    public static final String TASK_MORNING_PUSH = "morningPush";
    
    /**
     * 任务Key：课前提醒
     */
    public static final String TASK_COURSE_REMINDER = "courseReminder";
    
    /**
     * 任务Key：课程笔记
     */
    public static final String TASK_COURSE_NOTE = "courseNote";
    
    /**
     * 任务Key：每日汇总
     */
    public static final String TASK_DAILY_SUMMARY = "dailySummary";
    
    /**
     * 任务Key：月度报告
     */
    public static final String TASK_MONTHLY_REPORT = "monthlyReport";
    
    /**
     * 任务Key：健康提醒
     */
    public static final String TASK_HEALTH_REMINDER = "healthReminder";
    
    /**
     * 任务锁前缀
     */
    public static final String TASK_LOCK_PREFIX = "task:lock:";
    
    /**
     * 任务锁有效期（秒）
     */
    public static final int TASK_LOCK_EXPIRE = 300;

    // ==================== 缓存相关 ====================
    
    /**
     * 缓存Key前缀：用户Token
     */
    public static final String CACHE_TOKEN_PREFIX = "token:";
    
    /**
     * 缓存Key前缀：用户信息
     */
    public static final String CACHE_USER_PREFIX = "user:";
    
    /**
     * 缓存Key前缀：天气信息
     */
    public static final String CACHE_WEATHER_PREFIX = "weather:";
    
    /**
     * 缓存Key前缀：课程信息
     */
    public static final String CACHE_COURSE_PREFIX = "course:";
    
    /**
     * 缓存Key前缀：消费统计
     */
    public static final String CACHE_COST_PREFIX = "cost:";
    
    /**
     * 缓存过期时间（分钟）
     */
    public static final int CACHE_EXPIRE_MINUTES = 30;

    // ==================== 文件相关 ====================
    
    /**
     * 允许上传的Excel文件后缀
     */
    public static final String[] ALLOWED_EXCEL_SUFFIX = {".xlsx", ".xls"};

    // ==================== 消息相关 ====================
    
    /**
     * 消息类型：系统通知
     */
    public static final String NOTIFY_TYPE_SYSTEM = "system";
    
    /**
     * 消息类型：课程提醒
     */
    public static final String NOTIFY_TYPE_COURSE = "course";
    
    /**
     * 消息类型：早安推送
     */
    public static final String NOTIFY_TYPE_MORNING = "morning";
    
    /**
     * 消息类型：消费报告
     */
    public static final String NOTIFY_TYPE_COST_REPORT = "cost_report";
    
/**
     * 消息类型：AI笔记
     */
    public static final String NOTIFY_TYPE_NOTE = "note";

    /**
     * 消息类型：食谱推荐
     */
    public static final String NOTIFY_TYPE_DIET = "diet";

    /**
     * 消息状态：未读
     */
    public static final int NOTIFY_UNREAD = 0;
    
    /**
     * 消息状态：已读
     */
    public static final int NOTIFY_READ = 1;

    // ==================== 分页相关 ====================
    
    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;
    
    /**
     * 默认每页数量
     */
    public static final int DEFAULT_SIZE = 20;
    
    /**
     * 最大每页数量
     */
    public static final int MAX_SIZE = 100;
}
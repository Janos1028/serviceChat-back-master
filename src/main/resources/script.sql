create table admin
(
    id           int auto_increment
        primary key,
    username     varchar(20)  not null comment '登录账号',
    nickname     varchar(20)  not null comment '昵称',
    password     varchar(255) not null comment '密码',
    user_profile varchar(255) null comment '管理员头像'
)
    charset = utf8mb3;

create table message_type
(
    id   int auto_increment comment '消息类型编号'
        primary key,
    name varchar(20) null comment '消息类型名称'
)
    charset = utf8mb3;

create table private_chat_conversation
(
    id                varchar(64)                          not null comment '会话唯一ID'
        primary key,
    user_id_1         int                                  not null comment '普通用户ID',
    user_id_2         int                                  not null comment '支撑人员ID',
    create_time       datetime   default CURRENT_TIMESTAMP null comment '会话创建时间',
    end_time          datetime                             null comment '会话结束时间',
    is_active         tinyint(1) default 1                 null comment '状态: 0-已结束，1-进行中, 2-超时未响应，3-待确定，4-强制结束',
    service_domain_id int                                  null comment '服务域id',
    service_id        int                                  null comment '支撑服务团队id',
    score             tinyint(1)                           null comment '会话评分：1-5',
    service_name      varchar(64)                          null
)
    charset = utf8mb3;

create index session_user_1
    on private_chat_conversation (user_id_1);

create index session_user_2
    on private_chat_conversation (user_id_2);

create table private_msg_content
(
    id                int auto_increment comment '主键'
        primary key,
    from_id           int               not null comment '发送者ID',
    to_id             int               not null comment '接收者ID',
    content           text              null comment '消息内容',
    create_time       datetime          null comment '发送时间',
    message_type_id   int     default 1 null comment '消息类型: 1-文本，2-图片，3-文件, 4-开启，5-关闭，6-评价，7-确认',
    conversation_id   varchar(64)       null comment '会话ID',
    state             tinyint default 0 not null comment '消息状态：0-未读；1-已读，2-待确认，3-已解决，4-未解决',
    service_domain_id int               null comment '服务域id'
)
    charset = utf8mb3;

create index idx_from_to_state
    on private_msg_content (from_id, to_id, state);

create index private_ibfk_2
    on private_msg_content (to_id);

create table service_domain
(
    id   int auto_increment
        primary key,
    name varchar(64) null comment '名字'
)
    comment '服务域';

create table support_services
(
    id                int auto_increment comment '主键id'
        primary key,
    name              varchar(64) not null comment '支撑服务名称',
    service_domain_id int         null comment '服务域id'
)
    comment '支撑服务团队表';

create table user
(
    id                int auto_increment
        primary key,
    username          varchar(20)          not null comment '登录账号',
    nickname          varchar(20)          not null comment '昵称',
    password          varchar(255)         not null comment '密码',
    user_profile      varchar(255)         null comment '用户头像',
    user_state_id     int        default 2 null comment '用户状态id：1-在线；2-离线；3-暂时离开',
    is_enabled        tinyint(1) default 1 null comment '是否可用',
    is_locked         tinyint(1) default 0 null comment '是否被锁定',
    user_type_id      int        default 0 not null comment '用户类型id: 0-普通用户；1-支撑人员',
    service_domain_id int                  null comment '服务域id'
)
    charset = utf8mb3;

create index user_ibfk_1
    on user (user_state_id);

create table user_state
(
    id   int         not null
        primary key,
    name varchar(20) not null comment '状态名'
)
    charset = utf8mb3;

create table user_support_services
(
    id         int auto_increment
        primary key,
    user_id    int not null comment '用户ID (支撑人员)',
    service_id int not null comment '服务ID'
)
    comment '用户与支撑服务关联表';

create table user_type
(
    id        int         not null comment 'id'
        primary key,
    type_name varchar(20) not null comment '用户类型名称'
)
    comment '用户类型表';



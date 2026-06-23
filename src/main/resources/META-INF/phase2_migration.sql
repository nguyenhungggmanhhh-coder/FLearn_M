-- ============================================================
-- FLearn Phase 2 – Database Migration Script
-- Chạy trên SQL Server (FLearnDB) khi có lỗi column không tồn tại
-- ============================================================

-- ─────────────────────────────────────────
-- 1. Classroom – thêm cột InviteCodeVisible
-- ─────────────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[Classes]') AND name = 'InviteCodeVisible'
)
BEGIN
    ALTER TABLE [Classes]
    ADD [InviteCodeVisible] BIT NOT NULL DEFAULT 0;
    PRINT 'Added InviteCodeVisible to Classes';
END

-- ─────────────────────────────────────────
-- 2. Lessons – thêm AvailableFrom, Deadline
-- ─────────────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[Lessons]') AND name = 'AvailableFrom'
)
BEGIN
    ALTER TABLE [Lessons] ADD [AvailableFrom] DATETIME NULL;
    PRINT 'Added AvailableFrom to Lessons';
END

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[Lessons]') AND name = 'Deadline'
)
BEGIN
    ALTER TABLE [Lessons] ADD [Deadline] DATETIME NULL;
    PRINT 'Added Deadline to Lessons';
END

-- ─────────────────────────────────────────
-- 3. Materials – thêm AvailableFrom, Deadline
-- ─────────────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[Materials]') AND name = 'AvailableFrom'
)
BEGIN
    ALTER TABLE [Materials] ADD [AvailableFrom] DATETIME NULL;
    PRINT 'Added AvailableFrom to Materials';
END

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[Materials]') AND name = 'Deadline'
)
BEGIN
    ALTER TABLE [Materials] ADD [Deadline] DATETIME NULL;
    PRINT 'Added Deadline to Materials';
END

-- ─────────────────────────────────────────
-- 4. Bảng mới: ClassSchedules
-- ─────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'ClassSchedules' AND type = 'U')
BEGIN
    CREATE TABLE [ClassSchedules] (
        [ScheduleID]            INT IDENTITY(1,1) PRIMARY KEY,
        [ClassID]               INT NOT NULL,
        [DayOfWeek]             INT NOT NULL,            -- 1=CN, 2=T2, ..., 7=T7
        [StartTime]             TIME NOT NULL,
        [EndTime]               TIME NULL,
        [RoomOrLink]            NVARCHAR(500) NULL,
        [Note]                  NVARCHAR(500) NULL,
        [RemindOneDayBefore]    BIT NOT NULL DEFAULT 1,
        [RemindTwoHoursBefore]  BIT NOT NULL DEFAULT 1,
        [IsActive]              BIT NOT NULL DEFAULT 1,
        [CreatedAt]             DATETIME NOT NULL DEFAULT GETDATE(),
        [UpdatedAt]             DATETIME NULL,
        CONSTRAINT FK_Schedule_Class FOREIGN KEY ([ClassID]) REFERENCES [Classes]([ClassID])
    );
    PRINT 'Created ClassSchedules table';
END

-- ─────────────────────────────────────────
-- 5. Bảng mới: ReminderLogs
-- ─────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'ReminderLogs' AND type = 'U')
BEGIN
    CREATE TABLE [ReminderLogs] (
        [LogID]          INT IDENTITY(1,1) PRIMARY KEY,
        [ScheduleID]     INT NOT NULL,
        [UserID]         INT NOT NULL,
        [ReminderType]   NVARCHAR(20) NOT NULL,       -- '1DAY' | '2HOURS'
        [ScheduledDate]  DATE NOT NULL,               -- Ngày diễn ra buổi học
        [SentAt]         DATETIME NULL DEFAULT GETDATE(),
        [SentToEmail]    NVARCHAR(100) NULL,
        CONSTRAINT FK_ReminderLog_Schedule FOREIGN KEY ([ScheduleID]) REFERENCES [ClassSchedules]([ScheduleID]),
        CONSTRAINT FK_ReminderLog_User FOREIGN KEY ([UserID]) REFERENCES [Users]([UserID]),
        CONSTRAINT UQ_ReminderLog UNIQUE ([ScheduleID], [UserID], [ReminderType], [ScheduledDate])
    );
    PRINT 'Created ReminderLogs table';
END

-- ─────────────────────────────────────────
-- 6. Bảng mới: QuizSessionQuestions
-- ─────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'QuizSessionQuestions' AND type = 'U')
BEGIN
    CREATE TABLE [QuizSessionQuestions] (
        [SessionQuestionID] INT IDENTITY(1,1) PRIMARY KEY,
        [ResultID]          INT NOT NULL,
        [QuestionID]        INT NOT NULL,
        [DisplayOrder]      INT NOT NULL DEFAULT 0,
        CONSTRAINT FK_SessionQ_Result   FOREIGN KEY ([ResultID])   REFERENCES [QuizResults]([ResultID]),
        CONSTRAINT FK_SessionQ_Question FOREIGN KEY ([QuestionID]) REFERENCES [Questions]([QuestionID]),
        CONSTRAINT UQ_QuizSessionQuestion UNIQUE ([ResultID], [QuestionID])
    );
    PRINT 'Created QuizSessionQuestions table';
END

-- ─────────────────────────────────────────
-- 7. Bảng mới: PeerReviews
-- ─────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'PeerReviews' AND type = 'U')
BEGIN
    CREATE TABLE [PeerReviews] (
        [PeerReviewID]       INT IDENTITY(1,1) PRIMARY KEY,
        [RevieweeResultID]   INT NOT NULL,              -- Bài được chấm
        [ReviewerID]         INT NOT NULL,              -- Người chấm
        [Score]              FLOAT NULL,                -- 0.0 – 10.0
        [Comment]            NVARCHAR(MAX) NULL,
        [Status]             NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
        [Deadline]           DATETIME NULL,
        [AssignedAt]         DATETIME NOT NULL DEFAULT GETDATE(),
        [SubmittedAt]        DATETIME NULL,
        [DeviationFromMean]  FLOAT NULL,
        CONSTRAINT FK_PeerReview_Result   FOREIGN KEY ([RevieweeResultID]) REFERENCES [QuizResults]([ResultID]),
        CONSTRAINT FK_PeerReview_Reviewer FOREIGN KEY ([ReviewerID])       REFERENCES [Users]([UserID])
    );
    PRINT 'Created PeerReviews table';
END

-- ─────────────────────────────────────────
-- Kiểm tra kết quả
-- ─────────────────────────────────────────
SELECT 'Migration completed.' AS Result;
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('ClassSchedules','ReminderLogs','QuizSessionQuestions','PeerReviews')
ORDER BY TABLE_NAME;

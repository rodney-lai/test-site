CREATE TABLE IF NOT EXISTS "User_Reset_Password" (
  "User_Reset_Password_Id" UUID NOT NULL DEFAULT gen_random_uuid(),
  "User_Id" BIGINT NOT NULL,
  "Status" VARCHAR(20) NOT NULL,
  "Message_Id" UUID NULL,
  "Updated" TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  "Created" TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  CONSTRAINT "PK_User_Reset_Password" PRIMARY KEY ("User_Reset_Password_Id")
);

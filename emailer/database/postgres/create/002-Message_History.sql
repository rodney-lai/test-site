CREATE TABLE IF NOT EXISTS "Message_History" (
  "Message_History_Id" UUID NOT NULL DEFAULT gen_random_uuid(),
  "Delivery_Method" VARCHAR(20) NOT NULL,
  "Message_Type" VARCHAR(20) NOT NULL,
  "Message_Address" VARCHAR(256) NOT NULL,
  "Created" TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  CONSTRAINT "PK_Message_History" PRIMARY KEY ("Message_History_Id")
);

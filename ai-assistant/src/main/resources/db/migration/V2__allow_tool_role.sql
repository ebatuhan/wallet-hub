-- V2__allow_tool_role.sql

ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_role_check;

ALTER TABLE messages
ADD CONSTRAINT messages_role_check
CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'));
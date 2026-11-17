-- Admin-authored company knowledge injected into the AI assistant's prompt.
-- Empty by default; operators fill it from Settings > AI Assistant so the bot
-- can answer Britam-specific questions without a redeploy.
INSERT INTO app_settings (setting_group, setting_key, setting_value, data_type, description)
VALUES (
    'chatbot',
    'company_info',
    '',
    'STRING',
    'Company knowledge the assistant uses when answering customer questions'
)
ON CONFLICT (setting_group, setting_key) DO NOTHING;

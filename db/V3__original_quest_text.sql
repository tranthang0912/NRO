-- Restore the line breaks present in the original MySQL descriptions.
UPDATE task_main_template
SET detail = replace(replace(replace(detail, chr(13), ''), chr(10) || chr(10), chr(10)), chr(10), chr(13) || chr(10))
WHERE id IN (11, 14);

-- Keep the original non-payment NPCs and their map transitions.
UPDATE map_template SET npcs = '[[39,916,360],[6,180,288],[15,996,360],[80,1100,360]]' WHERE id = 20;
UPDATE map_template SET npcs = '["[55,330,230]","[56,285,230]","[20,420,240]"]' WHERE id = 48;
UPDATE map_template SET npcs = '[[44,268,330],[45,238,336],[43,400,336]]' WHERE id = 50;

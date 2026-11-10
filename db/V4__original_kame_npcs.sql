-- Restore the island NPC list from SRC_NRO.rar, including the task 11 pointer.
UPDATE map_template
SET npcs = '[[39,984,408],[13,1068,408],[21,1205,408],[54,1292,408]]'
WHERE id = 5;

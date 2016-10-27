ALTER TABLE gesuch
  ADD COLUMN familiensituation_erstgesuch_id VARCHAR(36) DEFAULT NULL;

ALTER TABLE gesuch_aud
  ADD COLUMN familiensituation_erstgesuch_id VARCHAR(36) DEFAULT NULL;

ALTER TABLE gesuch
  ADD CONSTRAINT FK_gesuch_familiensituation_erstgesuch_id
FOREIGN KEY (familiensituation_erstgesuch_id)
REFERENCES familiensituation (id);

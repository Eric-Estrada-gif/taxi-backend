CREATE TABLE IF NOT EXISTS dispositivo_push (
  usuario_id BIGINT NOT NULL,
  token VARCHAR(512) NOT NULL,
  actualizado DATETIME NOT NULL,
  PRIMARY KEY (usuario_id)
);

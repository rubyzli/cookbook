#!/bin/sh
# Prints SQL that moves photos from the old images folder into the database, for recipes whose
# image_url still points at /images/<file>. Run it once against each database, e.g.
#
#   scripts/images-to-sql.sh ~/cookbook-images | psql "$DATABASE_URL"
#
# Safe to run again: a photo is only inserted while some recipe still points at its old URL.
set -eu

dir="${1:-$HOME/cookbook-images}"

echo "BEGIN;"
for file in "$dir"/*; do
  [ -f "$file" ] || continue
  name=$(basename "$file")
  case "$name" in
    *.jpg | *.jpeg | *.JPG | *.JPEG) type=image/jpeg ;;
    *.png | *.PNG) type=image/png ;;
    *.gif | *.GIF) type=image/gif ;;
    *.webp | *.WEBP) type=image/webp ;;
    *) echo "Skipping $name: not a photo" >&2; continue ;;
  esac
  old=$(printf '/images/%s' "$name" | sed "s/'/''/g")
  cat <<SQL
WITH img AS (
    INSERT INTO image (id, content_type, data, created_at)
    SELECT gen_random_uuid(), '$type', decode('$(xxd -p "$file" | tr -d '\n')', 'hex'), now()
    WHERE EXISTS (SELECT 1 FROM recipe WHERE image_url = '$old')
    RETURNING id
)
UPDATE recipe SET image_url = '/api/images/' || img.id FROM img WHERE recipe.image_url = '$old';
SQL
done
echo "COMMIT;"

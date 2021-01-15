ALTER TABLE "attachmentmeta"
ADD COLUMN "language" varchar(254);

with
  "attachlang" as (
    select "m"."attachid", "m"."language", "c"."doclang"
    from "attachmentmeta" m
    inner join "attachment" a on "a"."attachid" = "m"."attachid"
    inner join "item" i on "a"."itemid" = "i"."itemid"
    inner join "collective" c on "c"."cid" = "i"."cid"
  )
update "attachmentmeta" as "m"
set "language" = "c"."doclang"
from "attachlang" c
where "m"."attachid" = "c"."attachid" and "m"."language" is null;

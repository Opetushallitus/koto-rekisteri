ALTER TABLE "yki_arvioija"
    ALTER COLUMN "henkilotunnus" DROP NOT NULL,
    ALTER COLUMN "sahkopostiosoite" DROP NOT NULL,
    ALTER COLUMN "katuosoite" DROP NOT NULL,
    ALTER COLUMN "postinumero" DROP NOT NULL,
    ALTER COLUMN "postitoimipaikka" DROP NOT NULL;

ALTER TABLE public.scenario_version
    ADD COLUMN card_image_url varchar(2048),
    ADD COLUMN hero_image_url varchar(2048),
    ADD COLUMN company_logo_url varchar(2048),
    ADD COLUMN overview_video_url varchar(2048),
    ADD CONSTRAINT chk_scenario_version_card_image_https
        CHECK (card_image_url IS NULL OR card_image_url ~ '^https://'),
    ADD CONSTRAINT chk_scenario_version_hero_image_https
        CHECK (hero_image_url IS NULL OR hero_image_url ~ '^https://'),
    ADD CONSTRAINT chk_scenario_version_company_logo_https
        CHECK (company_logo_url IS NULL OR company_logo_url ~ '^https://'),
    ADD CONSTRAINT chk_scenario_version_overview_video_wistia
        CHECK (
            overview_video_url IS NULL
            OR overview_video_url ~ '^https://fast\.wistia\.(net|com)/embed/iframe/[A-Za-z0-9]+$'
        );

ALTER TABLE public.scenario_version DISABLE TRIGGER guard_scenario_version;

UPDATE public.scenario_version AS version
SET card_image_url = media.card_image_url,
    hero_image_url = media.hero_image_url,
    company_logo_url = media.company_logo_url,
    overview_video_url = media.overview_video_url
FROM public.scenario AS scenario
JOIN (
    VALUES
        ('software-engineering-pcij', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195855445/3.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195855445/3.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1780727875756/HPE_logo_full-clr_pos_rgb[33] (1).png', 'https://fast.wistia.net/embed/iframe/fz7z4ag7zx'),
        ('service-desk-hoq5', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1700666908845/datacom-servicedesk-program.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1700666908845/datacom-servicedesk-program.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/kv4qop5ifl'),
        ('intro-cloud-yfvk', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1683852665368/Cloud.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1683852665368/Cloud.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', NULL),
        ('partnering-with-ai-in-the-workplace-khv2', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1756151216909/62.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1756151216909/62.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/zsx1qgzodj'),
        ('presales-hp1a', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195788252/1.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195788252/1.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1780727850109/HPE_logo_full-clr_pos_rgb[33] (1).png', 'https://fast.wistia.net/embed/iframe/fz7z4ag7zx'),
        ('digital-sales-mmkg', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195826534/2.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195826534/2.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1780727532897/HPE_logo_full-clr_pos_rgb[33] (1).png', 'https://fast.wistia.net/embed/iframe/fz7z4ag7zx'),
        ('automation-zn3l', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189709707/AdobeStock_124.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189709707/AdobeStock_124.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/80auu6mj6c'),
        ('cybersecurity-zm6d', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189669882/AdobeStock_759.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189669882/AdobeStock_759.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/pyotjryjel'),
        ('software-development-l47g', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189848270/AdobeStock_050.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189848270/AdobeStock_050.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/l040w8olx1')
) AS media(slug, card_image_url, hero_image_url, company_logo_url, overview_video_url)
    ON media.slug = scenario.slug
WHERE version.scenario_id = scenario.id;

ALTER TABLE public.scenario_version ENABLE TRIGGER guard_scenario_version;

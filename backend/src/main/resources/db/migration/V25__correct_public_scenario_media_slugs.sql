ALTER TABLE public.scenario_version DISABLE TRIGGER guard_scenario_version;

UPDATE public.scenario_version AS version
SET card_image_url = media.card_image_url,
    hero_image_url = media.hero_image_url,
    company_logo_url = media.company_logo_url,
    overview_video_url = media.overview_video_url
FROM public.scenario AS scenario
JOIN (
    VALUES
        ('hpe-software-engineering', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195855445/3.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195855445/3.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1780727875756/HPE_logo_full-clr_pos_rgb[33] (1).png', 'https://fast.wistia.net/embed/iframe/fz7z4ag7zx'),
        ('datacom-service-desk', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1700666908845/datacom-servicedesk-program.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1700666908845/datacom-servicedesk-program.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/kv4qop5ifl'),
        ('datacom-intro-cloud', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1683852665368/Cloud.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1683852665368/Cloud.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', NULL),
        ('datacom-partnering-with-ai', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1756151216909/62.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1756151216909/62.png', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/zsx1qgzodj'),
        ('hpe-presales', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195788252/1.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195788252/1.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1780727850109/HPE_logo_full-clr_pos_rgb[33] (1).png', 'https://fast.wistia.net/embed/iframe/fz7z4ag7zx'),
        ('hpe-digital-sales', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195826534/2.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1785195826534/2.png', 'https://cdn.theforage.com/vinternships/companyassets/fgHAi6dLhpRsGKyyN/t4qyCvg9x8dHJ5sEh/1780727532897/HPE_logo_full-clr_pos_rgb[33] (1).png', 'https://fast.wistia.net/embed/iframe/fz7z4ag7zx'),
        ('datacom-automation', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189709707/AdobeStock_124.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189709707/AdobeStock_124.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/80auu6mj6c'),
        ('datacom-cybersecurity', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189669882/AdobeStock_759.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189669882/AdobeStock_759.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/pyotjryjel'),
        ('datacom-software-development', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189848270/AdobeStock_050.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189848270/AdobeStock_050.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/l040w8olx1')
) AS media(slug, card_image_url, hero_image_url, company_logo_url, overview_video_url)
    ON media.slug = scenario.slug
WHERE version.scenario_id = scenario.id;

ALTER TABLE public.scenario_version ENABLE TRIGGER guard_scenario_version;

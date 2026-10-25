ALTER TABLE public.scenario_version DISABLE TRIGGER guard_scenario_version;

UPDATE public.scenario_version AS version
SET card_image_url = media.card_image_url,
    hero_image_url = media.hero_image_url,
    company_logo_url = media.company_logo_url,
    overview_video_url = media.overview_video_url
FROM public.scenario AS scenario
JOIN (
    VALUES
        ('datacom-introduction-to-cloud', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1683852665368/Cloud.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1683852665368/Cloud.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', NULL),
        ('datacom-automation-ai-accelerator', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189709707/AdobeStock_124.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189709707/AdobeStock_124.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/80auu6mj6c'),
        ('datacom-cyber-security-operations', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189669882/AdobeStock_759.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1686189669882/AdobeStock_759.jpeg', 'https://cdn.theforage.com/vinternships/companyassets/gCW7Xki5Y3vNpBmnn/3gcAGm4XfHPEeiGo3/1677531088428/datacom logo.png', 'https://fast.wistia.net/embed/iframe/pyotjryjel')
) AS media(slug, card_image_url, hero_image_url, company_logo_url, overview_video_url)
    ON media.slug = scenario.slug
WHERE version.scenario_id = scenario.id;

ALTER TABLE public.scenario_version ENABLE TRIGGER guard_scenario_version;

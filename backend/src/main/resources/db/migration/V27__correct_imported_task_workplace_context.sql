ALTER TABLE public.simulation_task DISABLE TRIGGER guard_content;

UPDATE public.simulation_task AS task
SET workplace_context = source.tagline
FROM public.scenario_version AS version
JOIN public.scenario AS scenario
    ON scenario.id = version.scenario_id
JOIN (
    VALUES
        ('hpe-software-engineering', 1, 'Explore how to use Java Spring Boot and the GreenLake cloud platform to design a RESTful web service'),
        ('hpe-software-engineering', 2, 'Using Java Spring Boot, build a basic RESTful Web Service that can send basic server requests'),
        ('hpe-software-engineering', 3, 'Add application support for uploading JSON data'),
        ('hpe-software-engineering', 4, 'Develop a set of unit tests to assess your application’s performance'),
        ('datacom-service-desk', 1, 'Help a Datacom client with their technical hurdles by resolving their email access issue with expert troubleshooting and communication'),
        ('datacom-service-desk', 2, 'Tackle a network outage with strategic prioritisation and clear client communication'),
        ('datacom-introduction-to-cloud', 1, 'Simulate your first day at work with Datacom'),
        ('datacom-introduction-to-cloud', 2, 'Experience the most used tool in the computer science world'),
        ('datacom-partnering-with-ai', 1, 'Start smart. Learn to ask better questions and get better results with AI.'),
        ('datacom-partnering-with-ai', 2, 'Turn AI insights into polished and visual presentations under pressure.'),
        ('datacom-partnering-with-ai', 3, 'Use AI to troubleshoot broken code even if you''re not a developer.'),
        ('hpe-presales', 1, 'Start researching potential solutions for a new presales opportunity'),
        ('hpe-presales', 2, 'Based on feedback from the customer and the sales team, prepare for your first discussion with the customer'),
        ('hpe-presales', 3, 'Prepare for internal sales team meeting to review solution options and discuss which product(s) should be proposed'),
        ('hpe-presales', 4, 'Based on your work and additional input from the sales team, put together a presentation outlining the proposed solution'),
        ('hpe-digital-sales', 1, 'Get familiar with HPE as a company'),
        ('hpe-digital-sales', 2, 'Kick off an HPE customer presentation'),
        ('hpe-digital-sales', 3, 'Understand your target customer and get your manager up to speed'),
        ('hpe-digital-sales', 4, ''),
        ('datacom-automation-ai-accelerator', 1, 'Learn what process automation is in order to meet your client''s needs'),
        ('datacom-automation-ai-accelerator', 2, 'Design a process flow for a key persona in the process automation design'),
        ('datacom-automation-ai-accelerator', 3, ''),
        ('datacom-cyber-security-operations', 1, ''),
        ('datacom-cyber-security-operations', 2, 'Conduct a comprehensive risk assessment'),
        ('datacom-software-development', 1, 'Review a web application and plan for future improvements'),
        ('datacom-software-development', 2, 'Identify the root cause of bugs and implementing a fix to improve the application')
) AS source(slug, position, tagline)
    ON source.slug = scenario.slug
WHERE task.scenario_version_id = version.id
  AND scenario.current_version_id = version.id
  AND task.position = source.position;

ALTER TABLE public.simulation_task ENABLE TRIGGER guard_content;

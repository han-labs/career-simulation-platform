ALTER TABLE public.scenario
    ADD COLUMN industry_code varchar(32);

ALTER TABLE public.scenario
    ADD CONSTRAINT ck_scenario_industry_code
    CHECK (industry_code IS NULL OR industry_code = 'IT');

import { createContext } from 'react'
import { createTranslator } from './translate.js'

export const LANGUAGE_KEY = 'careersim_ui_language_v1'
export const I18nContext = createContext({ locale: 'en', setLocale: () => {}, t: createTranslator('en') })

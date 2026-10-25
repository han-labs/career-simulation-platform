import { Outlet } from 'react-router-dom'
import PlatformNav from './PlatformNav.jsx'
import './adminStudio.css'

export default function AdminStudioLayout() {
  return <div className="admin-studio">
    <PlatformNav />
    <div className="admin-studio-content"><Outlet /></div>
  </div>
}

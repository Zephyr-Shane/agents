import { createRouter, createWebHashHistory } from 'vue-router'
import Dialogue from '@/pages/dialogue/dialogue.vue'
import Mine from '@/pages/mine/mine.vue'

const routes = [
  { path: '/', redirect: '/dialogue' },
  { path: '/dialogue', component: Dialogue },
  { path: '/mine', component: Mine }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})

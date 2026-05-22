import { createRouter, createWebHistory } from 'vue-router';
import AppShell from './layouts/AppShell.vue';
import PlannerView from './views/PlannerView.vue';
import LoginView from './views/LoginView.vue';
import RegisterView from './views/RegisterView.vue';
import ItineraryDetailView from './views/ItineraryDetailView.vue';
import HistoryView from './views/HistoryView.vue';
import ItineraryPrintView from './views/ItineraryPrintView.vue';
import { useAuthStore } from './stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/',
      component: AppShell,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/planner'
        },
        {
          path: 'planner',
          name: 'planner',
          component: PlannerView
        },
        {
          path: 'history',
          name: 'history',
          component: HistoryView
        },
        {
          path: 'itineraries/:id',
          name: 'itinerary-detail',
          component: ItineraryDetailView
        },
        {
          path: 'itineraries/:id/print',
          name: 'itinerary-print',
          component: ItineraryPrintView
        }
      ]
    }
  ]
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (!authStore.initialized) {
    await authStore.fetchMe();
  }

  if (to.meta.requiresAuth && !authStore.authenticated) {
    return { name: 'login' };
  }

  if ((to.name === 'login' || to.name === 'register') && authStore.authenticated) {
    return { name: 'planner' };
  }

  return true;
});

export default router;

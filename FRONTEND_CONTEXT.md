# NOVA Dance Studio CRM - Frontend Context

## 🎯 **Proyecto Overview**

### **¿Qué es NOVA Dance Studio CRM?**
Sistema integral de gestión para el estudio de danza NOVA que permite administrar estudiantes, profesoras, clases, horarios, inscripciones y procesamiento de pagos con reglas de negocio específicas para estudios de danza.

### **Problema que Resuelve**
- **Gestión manual ineficiente** de ~120 estudiantes
- **Seguimiento de pagos** complicado con recargos por mora
- **Cálculo manual** de compensaciones de profesoras (50% vs 100%)
- **Falta de reportes** para toma de decisiones
- **Inscripciones y horarios** desorganizados

### **Usuarios Objetivo**
- **Administradores del estudio** (dueños/gerentes)
- **Personal administrativo** (recepcionistas)
- **Profesoras** (consulta de horarios y compensaciones)

### **Métricas del Negocio**
- **~120 estudiantes** activos
- **20-30 clases** diferentes por semana
- **10 profesoras** con diferentes especialidades
- **120+ pagos** procesados mensualmente
- **Sistema de mora** del 15% después del día 10

## 🖥️ **Pantallas Requeridas**

### **1. Login & Autenticación**
```
/login
- Formulario de login (usuario/contraseña)
- Validación de credenciales
- Manejo de errores
- Redirección post-login
- Logout functionality
```

### **2. Dashboard Principal**
```
/dashboard
- KPIs principales:
  * Total estudiantes activos
  * Clases programadas hoy
  * Pagos pendientes este mes
  * Ingresos del mes actual
- Gráficos:
  * Ingresos por mes (últimos 6 meses)
  * Estudiantes por clase
  * Pagos vs Pagos con mora
- Accesos rápidos:
  * Procesar pago rápido
  * Ver pagos pendientes
  * Clases de hoy
- Notificaciones:
  * Pagos vencidos
  * Clases con pocos estudiantes
```

### **3. Gestión de Estudiantes**
```
/students
- Lista de estudiantes con:
  * Foto/avatar
  * Nombre completo
  * Email y teléfono
  * Clases inscritas
  * Estado de pagos
  * Acciones (editar, ver, eliminar)
- Filtros:
  * Por estado (activo/inactivo)
  * Por clase inscrita
  * Por estado de pago
- Búsqueda por nombre/email
- Paginación
- Botón "Nuevo Estudiante"

/students/new
- Formulario de creación:
  * Datos personales (nombre, email, teléfono, fecha nacimiento)
  * Inscripción a clases (múltiple selección)
  * Foto/avatar (opcional)
- Validaciones en tiempo real
- Preview de clases seleccionadas con precios

/students/:id
- Perfil completo del estudiante:
  * Información personal (editable)
  * Clases inscritas con horarios
  * Historial de pagos
  * Próximos pagos pendientes
- Acciones:
  * Inscribir en nueva clase
  * Desinscribir de clase
  * Procesar pago
  * Ver historial completo

/students/:id/edit
- Formulario de edición (similar a creación)
- Campos pre-poblados
- Validaciones
```

### **4. Gestión de Profesoras**
```
/teachers
- Lista de profesoras con:
  * Foto/avatar
  * Nombre completo
  * Especialidades (tags coloridos)
  * Clases asignadas
  * Estado (dueña del estudio destacado)
  * Compensación del mes
- Filtros por especialidad
- Búsqueda por nombre

/teachers/new
- Formulario de creación:
  * Datos personales
  * Especialidades (múltiple selección con chips)
  * Checkbox "Es dueña del estudio"
  * Foto/avatar

/teachers/:id
- Perfil de profesora:
  * Información personal
  * Clases asignadas con horarios
  * Estudiantes en sus clases
  * Compensación mensual (50% o 100%)
  * Historial de compensaciones
- Gráfico de ingresos por mes
```

### **5. Gestión de Clases**
```
/classes
- Vista de calendario semanal:
  * Clases por día de la semana
  * Horarios visuales
  * Profesora asignada
  * Número de estudiantes inscritos/máximo
- Vista de lista:
  * Nombre de clase
  * Profesora
  * Horario (día + hora)
  * Precio
  * Estudiantes (actual/máximo)
  * Estado

/classes/new
- Formulario de creación:
  * Información básica (nombre, descripción)
  * Selección de profesora
  * Horario (día de semana + hora inicio)
  * Duración (en minutos)
  * Precio por clase
  * Máximo de estudiantes
- Preview del horario en calendario

/classes/:id
- Detalle de clase:
  * Información completa
  * Lista de estudiantes inscritos
  * Horario en calendario visual
  * Estadísticas (asistencia, ingresos)
- Acciones:
  * Inscribir estudiante
  * Desinscribir estudiante
  * Editar información
```

### **6. Procesamiento de Pagos**
```
/payments
- Lista de pagos con:
  * Fecha de pago
  * Estudiante
  * Clases pagadas
  * Monto base + recargo
  * Estado
  * Mes correspondiente
- Filtros:
  * Por mes
  * Por estudiante
  * Con/sin recargo por mora
  * Por estado

/payments/new
- Formulario de pago:
  * Selección de estudiante (autocomplete)
  * Clases a pagar (múltiple selección)
  * Cálculo automático:
    - Precio base por clase
    - Descuento por pago múltiple (si aplica)
    - Recargo por mora (15% si es después del día 10)
  * Fecha de pago (por defecto hoy)
  * Método de pago
- Preview del recibo
- Confirmación antes de procesar

/payments/quick
- Pago rápido:
  * Búsqueda rápida de estudiante
  * Clases pendientes de pago automáticamente
  * Un click para procesar
```

### **7. Reportes y Analytics**
```
/reports
- Dashboard de reportes:
  * Selector de período (mes/trimestre/año)
  * Métricas principales
  * Gráficos interactivos

/reports/outstanding
- Pagos pendientes:
  * Lista de estudiantes con pagos vencidos
  * Monto adeudado
  * Días de retraso
  * Recargo calculado
  * Acciones (enviar recordatorio, procesar pago)

/reports/teacher-compensation
- Compensación de profesoras:
  * Por profesora o general
  * Desglose por clase
  * Comparativo mensual
  * Exportar a PDF/Excel

/reports/financial
- Resumen financiero:
  * Ingresos totales
  * Ingresos por clase
  * Comparativo con meses anteriores
  * Proyección de ingresos
  * Gráficos de tendencias
```

### **8. Configuración**
```
/settings
- Configuración general:
  * Información del estudio
  * Configuración de pagos (fecha límite, % recargo)
  * Usuarios y permisos
  * Backup de datos
```

## 🎨 **Especificaciones de UI/UX**

### **Design System**
- **Colores primarios:** Tonos de danza (rosa, púrpura, dorado)
- **Tipografía:** Moderna y legible (Inter, Roboto)
- **Iconografía:** Consistente (Heroicons, Lucide)
- **Espaciado:** Sistema de 8px
- **Bordes:** Redondeados (4px, 8px, 12px)

### **Componentes Reutilizables**
```javascript
// Componentes básicos
- Button (primary, secondary, danger, ghost)
- Input (text, email, tel, date, select)
- Modal (confirmation, form, info)
- Card (data display, actions)
- Badge (status, tags)
- Avatar (user photos)
- DataTable (sortable, filterable, paginated)

// Componentes específicos
- StudentCard
- TeacherCard
- ClassSchedule
- PaymentSummary
- KPIWidget
- Calendar
```

### **Estados y Feedback**
- **Loading states** en todas las operaciones
- **Success/Error notifications** con auto-dismiss
- **Confirmaciones** para acciones destructivas
- **Validación en tiempo real** en formularios
- **Empty states** con call-to-action
- **Skeleton loaders** para mejor UX

### **Responsive Design**
```css
/* Breakpoints */
- Mobile: 320px - 768px
- Tablet: 768px - 1024px
- Desktop: 1024px+

/* Layout adaptativo */
- Sidebar colapsable en mobile
- Cards stack en mobile
- Tablas se convierten en cards en mobile
- Navegación bottom en mobile
```

## 🔄 **Flujos de Usuario Principales**

### **1. Flujo de Inscripción de Nuevo Estudiante**
```
1. Dashboard → "Nuevo Estudiante"
2. Llenar datos personales
3. Seleccionar clases (múltiple)
4. Preview de costos
5. Confirmar inscripción
6. Opción de procesar primer pago
7. Confirmación con detalles
```

### **2. Flujo de Procesamiento de Pago**
```
1. Búsqueda de estudiante
2. Ver clases pendientes de pago
3. Seleccionar clases a pagar
4. Cálculo automático (base + mora si aplica)
5. Confirmación de montos
6. Procesar pago
7. Generar recibo
8. Actualizar estado del estudiante
```

### **3. Flujo de Creación de Clase**
```
1. Vista de horarios → "Nueva Clase"
2. Información básica de la clase
3. Asignación de profesora
4. Configuración de horario
5. Preview en calendario
6. Confirmación
7. Notificación a profesora (futuro)
```

### **4. Flujo de Reporte Mensual**
```
1. Reportes → Seleccionar mes
2. Ver métricas generales
3. Drill-down en datos específicos
4. Filtrar por categorías
5. Exportar datos
6. Compartir reporte
```

## 📱 **Funcionalidades Avanzadas**

### **Dashboard Inteligente**
- **Widgets personalizables** por usuario
- **Alertas automáticas** (pagos vencidos, clases llenas)
- **Accesos rápidos** contextuales
- **Métricas en tiempo real**

### **Gestión de Pagos Inteligente**
- **Cálculo automático de recargos** por mora
- **Detección de pagos múltiples** con descuentos
- **Recordatorios automáticos** (futuro)
- **Historial de pagos** con filtros avanzados

### **Calendario y Horarios**
- **Vista semanal/mensual** de clases
- **Detección de conflictos** de horarios
- **Capacidad visual** (estudiantes/máximo)
- **Drag & drop** para reagendar (futuro)

### **Reportes Dinámicos**
- **Filtros interactivos** por fecha, profesora, clase
- **Gráficos responsivos** con drill-down
- **Exportación** a PDF/Excel
- **Comparativos** período a período

## 🔧 **Configuración Técnica Detallada**

### **Variables de Entorno**
```env
# API Configuration
REACT_APP_API_URL=https://nova-crm-99ca.onrender.com
REACT_APP_API_TIMEOUT=30000

# Authentication
REACT_APP_JWT_STORAGE_KEY=nova_crm_token
REACT_APP_SESSION_TIMEOUT=86400000

# Features
REACT_APP_ENABLE_NOTIFICATIONS=true
REACT_APP_ENABLE_EXPORT=true
REACT_APP_ENABLE_DARK_MODE=true

# Analytics (futuro)
REACT_APP_ANALYTICS_ID=
```

### **Estructura de Carpetas Sugerida**
```
src/
├── components/
│   ├── common/          # Componentes reutilizables
│   ├── forms/           # Formularios específicos
│   ├── layout/          # Layout components
│   └── charts/          # Gráficos y visualizaciones
├── pages/               # Páginas principales
├── hooks/               # Custom hooks
├── services/            # API calls
├── utils/               # Utilidades
├── types/               # TypeScript types
├── constants/           # Constantes
└── styles/              # Estilos globales
```

### **Manejo de Estado**
```javascript
// Sugerencias de estado global
- Authentication state (user, token, permissions)
- Students data (list, filters, selected)
- Teachers data
- Classes data
- Payments data
- UI state (modals, notifications, loading)

// Herramientas recomendadas
- Zustand (simple y efectivo)
- React Query (para API calls)
- React Hook Form (para formularios)
```

### **Optimizaciones de Performance**
- **Lazy loading** de rutas
- **Virtualization** para listas grandes
- **Debouncing** en búsquedas
- **Caching** de datos con React Query
- **Image optimization** para avatars
- **Bundle splitting** por rutas

## 🎯 **Casos de Uso Específicos con UI**

### **Caso 1: Estudiante con Pago Atrasado**
```
Escenario: María González debe $5,000 desde el 15 de febrero
UI: 
- Badge rojo "PAGO VENCIDO" en su card
- Cálculo automático: $5,000 + $750 (15% mora) = $5,750
- Botón "Procesar Pago" prominente
- Modal de confirmación con desglose de costos
```

### **Caso 2: Profesora Dueña del Estudio**
```
Escenario: Ana Rodríguez es dueña, recibe 100% de compensación
UI:
- Crown icon junto a su nombre
- Badge dorado "DUEÑA DEL ESTUDIO"
- Compensación mostrada como 100% (no 50%)
- Acceso a reportes financieros completos
```

### **Caso 3: Clase Llena**
```
Escenario: Salsa Avanzada tiene 15/15 estudiantes
UI:
- Badge "LLENA" en rojo
- Barra de progreso al 100%
- Botón "Inscribir" deshabilitado
- Opción "Lista de espera" (futuro)
```

### **Caso 4: Pago Múltiple**
```
Escenario: Sofía quiere pagar 3 clases juntas
UI:
- Checkbox múltiple para seleccionar clases
- Cálculo dinámico del total
- Descuento automático si aplica
- Preview del recibo antes de confirmar
```

## 📊 **Métricas y KPIs a Mostrar**

### **Dashboard Principal**
- **Estudiantes Activos:** 120 (↑5 este mes)
- **Ingresos del Mes:** $540,000 (↑12% vs mes anterior)
- **Pagos Pendientes:** 15 estudiantes, $67,500 total
- **Clases Programadas Hoy:** 8 clases, 95 estudiantes
- **Ocupación Promedio:** 78% de capacidad

### **Reportes Financieros**
- **Ingresos por Clase:** Gráfico de barras
- **Tendencia de Pagos:** Línea temporal
- **Pagos con Mora:** Porcentaje mensual
- **Compensación por Profesora:** Tabla detallada
- **Proyección de Ingresos:** Basada en inscripciones

### **Analytics de Estudiantes**
- **Retención:** % de estudiantes que continúan
- **Clases Más Populares:** Ranking por inscripciones
- **Distribución de Edad:** Gráfico demográfico
- **Método de Pago Preferido:** Pie chart

---

## 🚀 **Instrucciones para la IA del Frontend**

**Contexto Completo:** Este es un CRM real para un estudio de danza con 120+ estudiantes activos. El backend está funcionando en producción con todas las funcionalidades implementadas.

**Prioridades de Desarrollo:**
1. **Autenticación y navegación** básica
2. **Dashboard** con métricas principales
3. **Gestión de estudiantes** (CRUD completo)
4. **Procesamiento de pagos** con cálculo de mora
5. **Gestión de clases y profesoras**
6. **Reportes básicos**

**Tecnologías Recomendadas:**
- **React 18 + TypeScript**
- **Tailwind CSS** para estilos
- **React Query** para API calls
- **React Hook Form** para formularios
- **Recharts** para gráficos
- **React Router** para navegación

**Consideraciones Especiales:**
- **Mobile-first** design (muchos usuarios usan tablets)
- **Validación robusta** (datos críticos de negocio)
- **Performance** (listas grandes de estudiantes)
- **Accesibilidad** (usuarios de diferentes edades)

**¡El backend está 100% funcional y listo para consumir!** 🎉

## 📊 **API Documentation**

### **GET /api/dashboard**
**Descripción:** Obtiene métricas principales del dashboard
**Autenticación:** Requerida (JWT)
**Respuesta:**
```json
{
  "kpis": {
    "totalStudents": 120,
    "totalTeachers": 10,
    "totalClasses": 25,
    "monthlyRevenue": 540000,
    "outstandingPayments": 15,
    "outstandingAmount": 67500,
    "classesToday": 25
  },
  "recentActivity": [
    {
      "id": 1,
      "studentName": "Sofía Martínez",
      "amount": 4500,
      "date": "2025-01-15",
      "status": "LATE"
    }
  ],
  "alerts": [
    {
      "type": "warning",
      "message": "15 pagos pendientes",
      "count": 15
    }
  ],
  "quickStats": {
    "averageClassSize": 4.8,
    "activeStudentsPercentage": 100,
    "monthlyGrowth": "+5%"
  }
}
```

### **GET /api/dashboard/revenue-chart**
**Descripción:** Obtiene datos de ingresos para gráficos
**Autenticación:** Requerida (JWT)
**Respuesta:**
```json
{
  "months": [
    {
      "month": "2024-09",
      "revenue": 480000,
      "payments": 110
    },
    {
      "month": "2024-10",
      "revenue": 520000,
      "payments": 115
    }
  ],
  "totalRevenue": 2940000,
  "averageMonthly": 490000
}
```

### **GET /api/dashboard/class-distribution**
**Descripción:** Obtiene distribución de estudiantes por clase
**Autenticación:** Requerida (JWT)
**Respuesta:**
```json
{
  "classes": [
    {
      "className": "Salsa Principiantes",
      "studentCount": 12,
      "maxStudents": 20,
      "occupancyPercentage": 60.0,
      "teacher": "Ana Rodríguez",
      "price": 4500
    }
  ],
  "totalCapacity": 300,
  "totalEnrolled": 240,
  "averageOccupancy": 80.0
}
```

### 🔐 Authentication Endpoints

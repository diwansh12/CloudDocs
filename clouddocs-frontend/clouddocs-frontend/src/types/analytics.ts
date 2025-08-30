export interface OverviewMetricsDTO {
  total: number;
  approved: number;
  rejected: number;
  inProgress: number;
  cancelled: number;
  overdueTasks: number;
  avgApprovalHours?: number | null;
  avgTaskCompletionHours?: number | null;
  totalTasksInPeriod: number;
  completedTasksInPeriod: number;
  completionRate?: number | null;
}

export interface TemplateMetricsDTO {
  templateId: string;
  templateName: string;
  total: number;
  approved: number;
  rejected: number;
  avgDurationHours?: number | null;
  approvalRate?: number | null;
}

export interface StepMetricsDTO {
  stepOrder: number;
  avgTaskCompletionHours?: number | null;
  approvals: number;
  rejections: number;
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  overdueTasks: number;
  completionRate?: number | null;
}

export interface MyMetricsDTO {
  myInitiatedTotal: number;
  myInitiatedApproved: number;
  myInitiatedRejected: number;
  myPendingTasks: number;
  myCompletedTasks: number;
  myAvgTaskCompletionHours?: number | null;
  myTaskCompletionRate?: number | null;
}

export interface DateRange {
  from: string;
  to: string;
}

export type ExportType = 'overview' | 'template' | 'step';

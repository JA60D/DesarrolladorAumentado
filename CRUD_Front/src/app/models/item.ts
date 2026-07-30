export interface Item {
  id?: number; // El id es opcional (?) porque al crear uno nuevo, la base de datos lo genera
  name: string;
  description: string;
  quantity: number;
}

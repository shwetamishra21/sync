import { useEffect, useState } from "react";
import { getHealth, type HealthResponse } from "../api/healthApi";

export function useHealth() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getHealth()
      .then(setHealth)
      .finally(() => setLoading(false));
  }, []);

  return {
    health,
    loading,
  };
}
import * as React from "react";

import { WithTranslation, withTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { makeStyles, Theme } from "@material-ui/core/styles";
import TextField from "@material-ui/core/TextField";
import Table from "@material-ui/core/Table";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import TableCell from "@material-ui/core/TableCell";
import TableBody from "@material-ui/core/TableBody";

import { useMaterialList } from "@/services/oni-save/hooks/useMaterials";
import { modifyMaterialQuantity } from "@/services/oni-save/actions/modify-material-quantity";

import DeleteLooseButton from "./DeleteLooseButton";

export interface MaterialsPageProps {
  className?: string;
}

type Props = MaterialsPageProps & WithTranslation;

const useStyles = makeStyles((theme: Theme) => ({
  searchBox: {
    margin: theme.spacing()
  },
  row: {
    height: "64px"
  }
}));

const MaterialsTable: React.FC<Props> = ({ className, t }) => {
  const styles = useStyles();
  const dispatch = useDispatch();
  const materials = useMaterialList();
  const [search, setSearch] = React.useState("");
  const onSearchChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setSearch(e.target.value.toLowerCase());
    },
    []
  );

  function formatWeight(weight: number) {
    if (Math.abs(weight) < 1000) {
      const g = Number(weight.toFixed(2));
      return t("material.gram", { count: g });
    }

    const kg = Number((weight / 1000.0).toFixed(2));
    return t("material.kilogram", { count: kg });
  }

  const handleLooseQuantityChange = React.useCallback(
    (materialType: string, newValue: number) => {
      if (isNaN(newValue) || newValue < 0) {
        return;
      }
      dispatch(modifyMaterialQuantity(materialType, true, newValue));
    },
    [dispatch]
  );

  const handleStoredQuantityChange = React.useCallback(
    (materialType: string, newValue: number) => {
      if (isNaN(newValue) || newValue < 0) {
        return;
      }
      dispatch(modifyMaterialQuantity(materialType, false, newValue));
    },
    [dispatch]
  );

  const displayMaterials = materials.filter(
    x => search === "" || x.name.toLowerCase().indexOf(search) !== -1
  );

  return (
    <div>
      <TextField
        className={styles.searchBox}
        label="Search"
        onChange={onSearchChange}
      />
      <Table className={className} size="small">
        <TableHead>
          <TableRow>
            <TableCell>{t("material.noun_titlecase")}</TableCell>
            <TableCell>
              <DeleteLooseButton />
              {t("material_loose.noun_titlecase")}
            </TableCell>
            <TableCell>{t("material_storage.noun_titlecase")}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {displayMaterials.map(
            ({ name, looseGrams, looseCount, storedGrams, storedCount }) => (
              <TableRow className={styles.row} key={name}>
                <TableCell>{name}</TableCell>
                <TableCell>
                  {looseGrams > 0 && (
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                      <DeleteLooseButton materialType={name} />
                      <TextField
                        type="number"
                        value={looseGrams}
                        onChange={e => {
                          const value = parseFloat(e.target.value);
                          if (!isNaN(value) && value >= 0) {
                            handleLooseQuantityChange(name, value);
                          }
                        }}
                        inputProps={{
                          min: 0,
                          step: 0.01,
                          style: { width: "100px" }
                        }}
                        size="small"
                      />
                      <span>g</span>
                      <span>|</span>
                      <span>{t("material_loose.clump_count", { count: looseCount })}</span>
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  {storedGrams > 0 && (
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                      <TextField
                        type="number"
                        value={storedGrams}
                        onChange={e => {
                          const value = parseFloat(e.target.value);
                          if (!isNaN(value) && value >= 0) {
                            handleStoredQuantityChange(name, value);
                          }
                        }}
                        inputProps={{
                          min: 0,
                          step: 0.01,
                          style: { width: "100px" }
                        }}
                        size="small"
                      />
                      <span>g</span>
                      <span>|</span>
                      <span>
                        {t("material_storage.container_count", {
                          count: storedCount
                        })}
                      </span>
                    </div>
                  )}
                </TableCell>
              </TableRow>
            )
          )}
        </TableBody>
      </Table>
    </div>
  );
};

export default withTranslation()(MaterialsTable);

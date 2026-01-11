import * as React from "react";
import classnames from "classnames";

import { Theme, makeStyles } from "@material-ui/core/styles";

import useDuplicants from "@/services/oni-save/hooks/useDuplicants";

import DuplicantListItem from "./DuplicantListItem";

export interface DuplicantListProps {
  className?: string;
}

const useStyles = makeStyles((theme: Theme) => ({
  root: {
    display: "flex",
    flexDirection: "row",
    flexWrap: "wrap",
    margin: theme.spacing()
  },
  item: {
    margin: theme.spacing(0.5)
  }
}));

type Props = DuplicantListProps;

const DuplicantList: React.FC<Props> = ({ className }) => {
  const classes = useStyles();
  const gameObjectIds = useDuplicants();
  return (
    <div className={classnames(className, classes.root)}>
      {gameObjectIds.map(id => (
        <DuplicantListItem
          key={id}
          className={classes.item}
          gameObjectId={id}
        />
      ))}
    </div>
  );
};

export default DuplicantList;

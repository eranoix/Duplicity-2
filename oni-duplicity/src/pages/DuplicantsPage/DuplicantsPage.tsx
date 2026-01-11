import * as React from "react";

import { WithTranslation, withTranslation } from "react-i18next";

import { makeStyles } from "@material-ui/core/styles";

import PageContainer from "@/components/PageContainer";
import RedirectIfNoSave from "@/components/RedirectIfNoSave";

import DuplicantList from "./components/DuplicantList";

const useStyles = makeStyles({
  duplicantList: {
    overflow: "auto"
  }
});

type Props = WithTranslation;

const DuplicantsPage: React.FC<Props> = ({ t }) => {
  const classes = useStyles();
  return (
    <PageContainer title={t("duplicant.noun_titlecase_plural")}>
      <RedirectIfNoSave />
      <DuplicantList className={classes.duplicantList} />
    </PageContainer>
  );
};
export default withTranslation()(DuplicantsPage);

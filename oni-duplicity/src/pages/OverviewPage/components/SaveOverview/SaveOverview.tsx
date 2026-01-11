import * as React from "react";
import { connect } from "react-redux";

import {
  Theme,
  createStyles,
  withStyles,
  WithStyles
} from "@material-ui/core/styles";
import Typography from "@material-ui/core/Typography";
import Divider from "@material-ui/core/Divider";

import PageContainer from "@/components/PageContainer";

import Difficulty from "./components/Difficulty";

import mapStateToProps, { StateProps } from "./state-props";

const styles = (theme: Theme) =>
  createStyles({
    root: {
      padding: theme.spacing()
    },
    difficulty: {
      marginTop: theme.spacing()
    },
    acknowledgments: {
      marginTop: theme.spacing(4),
      padding: theme.spacing(2),
      backgroundColor: theme.palette.background.paper,
      borderRadius: theme.shape.borderRadius,
      border: `1px solid ${theme.palette.divider}`,
    },
    acknowledgmentsText: {
      marginTop: theme.spacing(1),
    },
  });

type Props = StateProps & WithStyles<typeof styles>;

const SaveOverview: React.FC<Props> = ({ classes, saveName, cycleCount }) => (
  <PageContainer title="Overview">
    <div className={classes.root}>
      <Typography variant="h4">{saveName}</Typography>
      <Divider />
      <Typography>{cycleCount} cycles.</Typography>
      <Difficulty className={classes.difficulty} />
      <div className={classes.acknowledgments}>
        <Typography variant="h6">Acknowledgments</Typography>
        <Typography variant="body2" className={classes.acknowledgmentsText}>
          We would like to express our gratitude to{" "}
          <a
            href="https://github.com/RoboPhred"
            target="_blank"
            rel="noopener noreferrer"
          >
            RoboPhred
          </a>{" "}
          and all the contributors who created and open-sourced the original{" "}
          <a
            href="https://robophred.github.io/oni-duplicity/"
            target="_blank"
            rel="noopener noreferrer"
          >
            Duplicity
          </a>{" "}
          project. This project would not have been possible without their
          excellent work and dedication to the Oxygen Not Included community.
          Thank you for making the source code available and allowing others
          to build upon your foundation.
        </Typography>
      </div>
    </div>
  </PageContainer>
);

export default connect(mapStateToProps)(withStyles(styles)(SaveOverview));

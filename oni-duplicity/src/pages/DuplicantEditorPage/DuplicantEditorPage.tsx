import * as React from "react";

import { RouteComponentProps } from "react-router";

import useIsEditableDuplicant from "@/services/oni-save/hooks/useIsEditableDuplicant";

import RedirectIfNoSave from "@/components/RedirectIfNoSave";

import DuplicantEditor from "./components/DuplicantEditor";
import DuplicantNotFound from "./components/DuplicantNotFound";

export interface DuplicantEditorRouteParams {
  gameObjectId: string;
}

export interface DuplicantEditorProps
  extends RouteComponentProps<DuplicantEditorRouteParams> {}

type Props = DuplicantEditorProps;
const DuplicantEditorPage: React.FC<Props> = ({
  match: {
    params: { gameObjectId }
  }
}) => {
  const numericGameObjectId = Number(gameObjectId);
  const isEditable = useIsEditableDuplicant(numericGameObjectId);
  // Verifica se o objeto tem Accessorizer e MinionIdentity behaviors
  // Isso inclui tanto duplicantes humanos quanto bionics
  return (
    <>
      <RedirectIfNoSave />
      {isEditable && (
        <DuplicantEditor gameObjectId={numericGameObjectId} />
      )}
      {!isEditable && <DuplicantNotFound />}
    </>
  );
};

export default DuplicantEditorPage;

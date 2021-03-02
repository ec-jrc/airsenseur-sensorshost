/* ===========================================================================
 * Copyright 2015 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or subsequent versions of the
 * EUPL (the "License"); You may not use this work except in compliance
 * with the License. You may obtain a copy of the License at
 * http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Date: 02/04/2015
 * Authors:
 * - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu,
 *   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and
 *   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
 *			European Commission - Joint Research Centre,
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * ===========================================================================
 */

import React from 'react';
import { Formik, Form } from "formik";
import { toast } from "react-toastify";

import useDataSource from '../hooks/useDataSource';
import LoaderSpinner from '../utils/LoaderSpinner';
import SimplePage from './templates/SimplePage';
import ActionButtonsContainer from './templates/ActionButtonsContainer';
import useAsyncAction from '../hooks/useAsyncAction';
import Card from './templates/Card';

const FormContainerHOC = (  WrappedComponent,
                            apiUrl, 
                            validationSchema, 
                            pageTitle,
                            successMessage = "The configuration has been saved.",
                            preSaveOps = () => new Promise((resolve) => resolve(true))
                        ) => function FormContainer(props) {
                                
    const [initialValues, setInitialValues, initialValuesStatus] = useDataSource(apiUrl);
    const [triggerSave, saveStatus] = useAsyncAction(apiUrl, "POST");

    const status = [initialValuesStatus, saveStatus].includes("rejected") ? "rejected" 
                 : [initialValuesStatus, saveStatus].includes("pending") ? "pending"
                 : "";

    return (
        <Formik 
            enableReinitialize
            initialValues={initialValues}
            validationSchema={validationSchema}
            onSubmit={(values, { setSubmitting }) => {
                preSaveOps(values)
                .then(() => {
                    triggerSave(values, () => {
                        toast.success(successMessage);
                    });

                    setSubmitting(false);
                    setInitialValues(values);
                })
                .catch(() => {
                    setSubmitting(false);
                });
            }}>

            {(formik) => 
                <Form>        
                    <SimplePage title={pageTitle} className="FormContainer">
                        <Card>
                            <LoaderSpinner status={status}>
                                <WrappedComponent {...props} formik={formik} />
                            </LoaderSpinner>
                        </Card>

                        <ActionButtonsContainer>
                            <button type="reset" disabled={formik.isSubmitting}>Reset</button>
                            <button type="submit" disabled={formik.isSubmitting || !formik.isValid}>Save</button>
                        </ActionButtonsContainer>
                    </SimplePage>
                </Form>
            }
        </Formik>
    );
};

export default FormContainerHOC;
